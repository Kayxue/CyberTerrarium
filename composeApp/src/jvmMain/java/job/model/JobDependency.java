package job.model;

public class JobDependency {
    private String upstreamJobId;

    public JobDependency() {}

    public JobDependency(String upstreamJobId) {
        this.upstreamJobId = upstreamJobId;
    }

    public String getUpstreamJobId() {
        return upstreamJobId;
    }

    public void setUpstreamJobId(String upstreamJobId) {
        this.upstreamJobId = upstreamJobId;
    }
}
