package job.model.stage;

public class FlowStage {
    private String id;
    private String flowId;
    private String displayName;
    private int order;
    private BarrierMode barrierMode;
    private StageFailMode failMode;

    public FlowStage() {
        this.barrierMode = BarrierMode.SOFT;
        this.failMode = StageFailMode.STOP;
    }

    public FlowStage(
        String id,
        String flowId,
        String displayName,
        int order,
        BarrierMode barrierMode,
        StageFailMode failMode
    ) {
        this.id = id;
        this.flowId = flowId;
        this.displayName = displayName;
        this.order = order;
        this.barrierMode = barrierMode;
        this.failMode = failMode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public BarrierMode getBarrierMode() {
        return barrierMode;
    }

    public void setBarrierMode(BarrierMode barrierMode) {
        this.barrierMode = barrierMode;
    }

    public StageFailMode getFailMode() {
        return failMode;
    }

    public void setFailMode(StageFailMode failMode) {
        this.failMode = failMode;
    }
}
