package job.core;

import job.model.Job;
import job.model.result.FlowRunJob;
import job.model.result.FlowRun;
import job.model.result.FlowStatus;
import job.model.result.JobStatus;
import job.repository.IFlowRunRepository;
import job.repository.IFlowRunJobRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Executor {
    private final FlowExecutionPlanner flowExecutionPlanner;
    private final Scheduler scheduler;
    private final Dispatcher dispatcher;
    private final IFlowRunRepository flowRunRepository;
    private final IFlowRunJobRepository flowRunJobRepository;

    public Executor(
        FlowExecutionPlanner flowExecutionPlanner,
        Scheduler scheduler,
        Dispatcher dispatcher,
        IFlowRunRepository flowRunRepository,
        IFlowRunJobRepository flowRunJobRepository
    ) {
        this.flowExecutionPlanner = flowExecutionPlanner;
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
        this.flowRunRepository = flowRunRepository;
        this.flowRunJobRepository = flowRunJobRepository;
    }

    public FlowRun execute(String flowId, int maxWorkers) {
        FlowExecutionPlan plan = flowExecutionPlanner.plan(flowId);
        FlowRun run = new FlowRun(flowId, FlowStatus.PENDING);
        run.setStartedAt(Instant.now());
        flowRunRepository.save(run);

        Map<String, JobStatus> statuses = new HashMap<>();
        for (Job job : plan.getJobs()) {
            statuses.put(job.getId(), JobStatus.IDLE);
        }

        boolean error = false;
        while (!scheduler.allDone(plan.getJobsById().keySet(), statuses)) {
            List<Job> next = scheduler.nextBatch(plan, statuses, Math.max(1, maxWorkers));
            if (next.isEmpty()) {
                error = true;
                break;
            }
            for (Job job : next) {
                statuses.put(job.getId(), JobStatus.QUEUED);
            }
            Map<String, JobDispatchResult> batchResults = dispatcher.dispatch(next, maxWorkers);
            for (JobDispatchResult batchResult : batchResults.values()) {
                statuses.put(batchResult.getJobId(), batchResult.getStatus());
                FlowRunJob runJob = new FlowRunJob();
                runJob.setRunId(run.getId());
                runJob.setJobId(batchResult.getJobId());
                runJob.setStatus(batchResult.getStatus());
                runJob.setExitCode(batchResult.getExitCode());
                runJob.setStdoutText(batchResult.getStdout());
                runJob.setStderrText(batchResult.getStderr());
                runJob.setErrorMessage(batchResult.getErrorMessage());
                runJob.setStartedAt(batchResult.getStartedAt());
                runJob.setEndedAt(batchResult.getEndedAt());
                runJob.setDurationMs(batchResult.getDurationMs());
                flowRunJobRepository.save(runJob);
            }
        }

        if (!error && scheduler.allSucceeded(plan.getJobsById().keySet(), statuses)) {
            run.setStatus(FlowStatus.SUCCESS);
        } else {
            run.setStatus(FlowStatus.ERROR);
        }
        run.setEndedAt(Instant.now());
        flowRunRepository.updateOneById(run.getId(), run);
        return run;
    }
}
