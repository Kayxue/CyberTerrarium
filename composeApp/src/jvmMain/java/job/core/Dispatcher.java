package job.core;

import job.model.Job;
import job.model.JobConfig;
import job.model.result.JobStatus;
import job.model.script.ScriptLanguage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Dispatcher {
    public Map<String, JobDispatchResult> dispatch(List<Job> jobs, int maxWorkers) {
        int workerCount = Math.max(1, maxWorkers);
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        Map<String, Future<JobDispatchResult>> futures = new HashMap<>();
        for (Job job : jobs) {
            futures.put(job.getId(), pool.submit(new JobWorker(job)));
        }

        Map<String, JobDispatchResult> results = new HashMap<>();
        for (Map.Entry<String, Future<JobDispatchResult>> entry : futures.entrySet()) {
            try {
                results.put(entry.getKey(), entry.getValue().get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Instant now = Instant.now();
                results.put(entry.getKey(), new JobDispatchResult(
                    entry.getKey(),
                    JobStatus.CANCELLED,
                    -1,
                    "",
                    "",
                    "Interrupted while waiting for job result",
                    now,
                    now,
                    0L
                ));
            } catch (ExecutionException e) {
                Instant now = Instant.now();
                String message = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
                results.put(entry.getKey(), new JobDispatchResult(
                    entry.getKey(),
                    JobStatus.FAILED,
                    -1,
                    "",
                    "",
                    message == null ? "Execution failed" : message,
                    now,
                    now,
                    0L
                ));
            }
        }

        pool.shutdown();
        return results;
    }

    private static final class JobWorker implements Callable<JobDispatchResult> {
        private final Job job;

        private JobWorker(Job job) {
            this.job = job;
        }

        @Override
        public JobDispatchResult call() {
            return runScript(job);
        }
    }

    private static JobDispatchResult runScript(Job job) {
        Instant startedAt = Instant.now();
        try {
            Path tempFile = writeScriptToTemp(job);
            ProcessBuilder pb = new ProcessBuilder(buildCommand(job, tempFile));
            Map<String, String> env = pb.environment();
            JobConfig config = job.getConfig();
            if (config != null && config.getAttributes() != null) {
                env.putAll(config.getAttributes());
            }
            Process process = pb.start();
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

            Duration timeout = config == null || config.getTimeout() == null
                ? Duration.ofSeconds(60)
                : config.getTimeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
                Instant endedAt = Instant.now();
                String stdout = collectOutput(stdoutFuture);
                String stderr = collectOutput(stderrFuture);
                return new JobDispatchResult(
                    job.getId(),
                    JobStatus.TIMEOUT,
                    -1,
                    stdout,
                    stderr,
                    "Process timed out after " + timeout.toMillis() + " ms",
                    startedAt,
                    endedAt,
                    Duration.between(startedAt, endedAt).toMillis()
                );
            }
            int exitCode = process.exitValue();
            Instant endedAt = Instant.now();
            String stdout = collectOutput(stdoutFuture);
            String stderr = collectOutput(stderrFuture);
            JobStatus status = exitCode == 0 ? JobStatus.SUCCESS : JobStatus.FAILED;
            return new JobDispatchResult(
                job.getId(),
                status,
                exitCode,
                stdout,
                stderr,
                status == JobStatus.SUCCESS ? "" : "Process exited with code " + exitCode,
                startedAt,
                endedAt,
                Duration.between(startedAt, endedAt).toMillis()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Instant endedAt = Instant.now();
            return new JobDispatchResult(
                job.getId(),
                JobStatus.CANCELLED,
                -1,
                "",
                "",
                "Job execution interrupted",
                startedAt,
                endedAt,
                Duration.between(startedAt, endedAt).toMillis()
            );
        } catch (IOException e) {
            Instant endedAt = Instant.now();
            return new JobDispatchResult(
                job.getId(),
                JobStatus.FAILED,
                -1,
                "",
                "",
                "Failed to execute process: " + e.getMessage(),
                startedAt,
                endedAt,
                Duration.between(startedAt, endedAt).toMillis()
            );
        }
    }

    private static Path writeScriptToTemp(Job job) throws IOException {
        ScriptLanguage language = job.getScript().getLanguage();
        String suffix = switch (language) {
            case JAVA -> ".java";
            case PYTHON -> ".py";
            case C -> ".c";
            case CPP -> ".cpp";
            case LUA -> ".lua";
            case SHELL -> ".sh";
        };
        Path file = Files.createTempFile("flow-job-" + job.getId() + "-", suffix);
        Files.writeString(file, job.getScript().getContent(), StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException ignored) {
            // Windows or non-posix fs.
        }
        return file;
    }

    private static List<String> buildCommand(Job job, Path scriptFile) throws IOException {
        ScriptLanguage language = job.getScript().getLanguage();
        return switch (language) {
            case JAVA -> List.of("java", scriptFile.toString());
            case PYTHON -> List.of("python3", scriptFile.toString());
            case SHELL -> List.of("sh", scriptFile.toString());
            case LUA -> List.of("lua", scriptFile.toString());
            case C -> {
                Path out = Files.createTempFile("flow-job-c-bin-", "");
                yield List.of("sh", "-c",
                    "cc " + shellEscape(scriptFile.toString()) + " -o " + shellEscape(out.toString()) +
                        " && " + shellEscape(out.toString()));
            }
            case CPP -> {
                Path out = Files.createTempFile("flow-job-cpp-bin-", "");
                yield List.of("sh", "-c",
                    "c++ " + shellEscape(scriptFile.toString()) + " -o " + shellEscape(out.toString()) +
                        " && " + shellEscape(out.toString()));
            }
        };
    }

    private static String readStream(InputStream inputStream) {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String collectOutput(CompletableFuture<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            return future.getNow("");
        }
    }

    private static String shellEscape(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
