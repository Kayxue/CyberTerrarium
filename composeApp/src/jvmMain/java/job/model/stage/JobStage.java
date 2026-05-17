package job.model.stage;

public class JobStage {
    private String id;
    private String displayName;
    private int order;
    private BarrierMode barrierMode;
    private StageFailMode failMode;

    public JobStage() {
        this.barrierMode = BarrierMode.SOFT;
        this.failMode = StageFailMode.STOP;
    }

    public JobStage(String id, String displayName, int order, BarrierMode barrierMode, StageFailMode failMode) {
        this.id = id;
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
