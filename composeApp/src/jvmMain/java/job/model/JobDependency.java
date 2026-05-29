package job.model;

public class JobDependency {
    private String jobId;
    private String upstreamJobId;
    private double bendX;
    private double bendY;

    public JobDependency() {
        this.bendX = -1d;
        this.bendY = -1d;
    }

    public JobDependency(String jobId, String upstreamJobId) {
        this(jobId, upstreamJobId, -1d, -1d);
    }

    public JobDependency(String jobId, String upstreamJobId, double bendX, double bendY) {
        this.jobId = jobId;
        this.upstreamJobId = upstreamJobId;
        this.bendX = bendX;
        this.bendY = bendY;
    }

    public JobDependency(String upstreamJobId) {
        this("", upstreamJobId, -1d, -1d);
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

    public double getBendX() {
        return bendX;
    }

    public void setBendX(double bendX) {
        this.bendX = bendX;
    }

    public double getBendY() {
        return bendY;
    }

    public void setBendY(double bendY) {
        this.bendY = bendY;
    }
}
