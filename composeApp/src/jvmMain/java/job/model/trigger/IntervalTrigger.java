package job.model.trigger;

import java.time.Duration;

public final class IntervalTrigger implements FlowTrigger {
    private Duration duration;

    public IntervalTrigger() {
        this.duration = Duration.ofMinutes(1);
    }

    public IntervalTrigger(Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
