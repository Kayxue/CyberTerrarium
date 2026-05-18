package job.core;

import job.model.Job;
import job.model.JobConfig;
import job.model.result.JobStatus;
import job.model.script.ScriptLanguage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Dispatcher {
    public Map<String, JobStatus> dispatch(List<Job> jobs, int maxWorkers) {
        int workerCount = Math.max(1, maxWorkers);
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        Map<String, Future<JobStatus>> futures = new HashMap<>();
        for (Job job : jobs) {
            futures.put(job.getId(), pool.submit(new JobWorker(job)));
        }

        Map<String, JobStatus> results = new HashMap<>();
        for (Map.Entry<String, Future<JobStatus>> entry : futures.entrySet()) {
            try {
                results.put(entry.getKey(), entry.getValue().get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.put(entry.getKey(), JobStatus.CANCELLED);
            } catch (ExecutionException e) {
                results.put(entry.getKey(), JobStatus.FAILED);
            }
        }

        pool.shutdown();
        return results;
    }

    private static final class JobWorker implements Callable<JobStatus> {
        private final Job job;

        private JobWorker(Job job) {
            this.job = job;
        }

        @Override
        public JobStatus call() {
            return runScript(job);
        }
    }

    private static JobStatus runScript(Job job) {
        try {
            Path tempFile = writeScriptToTemp(job);
            ProcessBuilder pb = new ProcessBuilder(buildCommand(job, tempFile));
            Map<String, String> env = pb.environment();
            JobConfig config = job.getConfig();
            if (config != null && config.getAttributes() != null) {
                env.putAll(config.getAttributes());
            }
            Process process = pb.start();
            Duration timeout = config == null || config.getTimeout() == null
                ? Duration.ofSeconds(60)
                : config.getTimeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return JobStatus.TIMEOUT;
            }
            int exitCode = process.exitValue();
            return exitCode == 0 ? JobStatus.SUCCESS : JobStatus.FAILED;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return JobStatus.FAILED;
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

    private static String shellEscape(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
