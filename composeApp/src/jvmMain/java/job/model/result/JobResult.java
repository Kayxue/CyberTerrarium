package job.model.result;

import job.model.Job;

import java.time.Instant;

public class JobResult {
    private String jobId;
    private JobStatus status;
    private Instant endedAt;

    public JobResult() {}

    public JobResult(String jobId, JobStatus status) {
        this.jobId = jobId;
        this.status = status;
        this.endedAt = Instant.now();
    }

    public JobResult(Job job, JobStatus status) {
        this.jobId = job.getId();
        this.status = status;
        this.endedAt = Instant.now();
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

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
