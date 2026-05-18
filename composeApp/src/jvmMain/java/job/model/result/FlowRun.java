package job.model.result;

import java.time.Instant;

public class FlowRun {
    private long id;
    private String flowId;
    private FlowStatus status;
    private Instant startedAt;
    private Instant endedAt;

    public FlowRun() {}

    public FlowRun(String flowId, FlowStatus status) {
        this.flowId = flowId;
        this.status = status;
        this.startedAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public FlowStatus getStatus() {
        return status;
    }

    public void setStatus(FlowStatus status) {
        this.status = status;
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
}
