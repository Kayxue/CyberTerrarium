package job.model.trigger;

public sealed interface FlowTrigger permits IntervalTrigger, ManualTrigger { }
