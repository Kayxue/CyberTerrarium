package job.model.flow;

public class FlowJobLink {
    private String flowId;
    private String jobId;
    private int position;
    private double stageRelativeX;
    private double stageRelativeY;

    public FlowJobLink() {
        this.stageRelativeX = -1d;
        this.stageRelativeY = -1d;
    }

    public FlowJobLink(String flowId, String jobId, int position) {
        this(flowId, jobId, position, -1d, -1d);
    }

    public FlowJobLink(String flowId, String jobId, int position, double stageRelativeX, double stageRelativeY) {
        this.flowId = flowId;
        this.jobId = jobId;
        this.position = position;
        this.stageRelativeX = stageRelativeX;
        this.stageRelativeY = stageRelativeY;
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

    public double getStageRelativeX() {
        return stageRelativeX;
    }

    public void setStageRelativeX(double stageRelativeX) {
        this.stageRelativeX = stageRelativeX;
    }

    public double getStageRelativeY() {
        return stageRelativeY;
    }

    public void setStageRelativeY(double stageRelativeY) {
        this.stageRelativeY = stageRelativeY;
    }
}
