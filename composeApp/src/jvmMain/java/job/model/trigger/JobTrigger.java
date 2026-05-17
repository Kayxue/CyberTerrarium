package job.model.trigger;

public sealed interface JobTrigger permits IntervalTrigger, ManualTrigger { }
