package job.core;

import job.model.result.JobStatus;

import java.time.Instant;

public class JobDispatchResult {
    private final String jobId;
    private final JobStatus status;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String errorMessage;
    private final Instant startedAt;
    private final Instant endedAt;
    private final long durationMs;

    public JobDispatchResult(
        String jobId,
        JobStatus status,
        int exitCode,
        String stdout,
        String stderr,
        String errorMessage,
        Instant startedAt,
        Instant endedAt,
        long durationMs
    ) {
        this.jobId = jobId;
        this.status = status;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationMs = durationMs;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }
}

