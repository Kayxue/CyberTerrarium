package job.model;

public class JobDependency {
    private String jobId;
    private String upstreamJobId;

    public JobDependency() {}

    public JobDependency(String jobId, String upstreamJobId) {
        this.jobId = jobId;
        this.upstreamJobId = upstreamJobId;
    }

    public JobDependency(String upstreamJobId) {
        this.upstreamJobId = upstreamJobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUpstreamJobId() {
        return upstreamJobId;
    }

    public void setUpstreamJobId(String upstreamJobId) {
        this.upstreamJobId = upstreamJobId;
    }
}
