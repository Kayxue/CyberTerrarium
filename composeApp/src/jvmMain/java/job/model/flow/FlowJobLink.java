package job.model.flow;

public class FlowJobLink {
    private String flowId;
    private String jobId;
    private int position;

    public FlowJobLink() {}

    public FlowJobLink(String flowId, String jobId, int position) {
        this.flowId = flowId;
        this.jobId = jobId;
        this.position = position;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

