package job.model.result;

import java.time.Instant;

public class FlowRunJob {
    private long runId;
    private String jobId;
    private JobStatus status;
    private int exitCode;
    private String stdoutText;
    private String stderrText;
    private String errorMessage;
    private Instant startedAt;
    private Instant endedAt;
    private long durationMs;

    public FlowRunJob() {}

    public FlowRunJob(long runId, String jobId, JobStatus status) {
        this.runId = runId;
        this.jobId = jobId;
        this.status = status;
        this.exitCode = -1;
        this.stdoutText = "";
        this.stderrText = "";
        this.errorMessage = "";
        this.startedAt = Instant.now();
        this.endedAt = this.startedAt;
        this.durationMs = 0L;
    }

    public long getRunId() {
        return runId;
    }

    public void setRunId(long runId) {
        this.runId = runId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStdoutText() {
        return stdoutText;
    }

    public void setStdoutText(String stdoutText) {
        this.stdoutText = stdoutText;
    }

    public String getStderrText() {
        return stderrText;
    }

    public void setStderrText(String stderrText) {
        this.stderrText = stderrText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}

