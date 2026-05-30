package job.core;

import job.model.Job;
import job.model.JobConfig;
import job.model.result.JobStatus;
import job.model.stage.BarrierMode;
import job.model.stage.FlowStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.time.Duration;

public class Scheduler {
    public List<Job> nextBatch(
        FlowExecutionPlan plan,
        Map<String, JobStatus> statuses,
        int maxWorkers
    ) {
        OptionalInt hardOrder = firstUnfinishedHardStageOrder(plan, statuses);
        List<Job> readyJobs = new ArrayList<>();
        for (Job job : plan.getJobs()) {
            JobStatus current = statuses.getOrDefault(job.getId(), JobStatus.IDLE);
            if (current != JobStatus.IDLE) {
                continue;
            }
            if (!dependenciesSatisfied(job.getId(), plan.getDependencyMap(), statuses)) {
                continue;
            }
            if (hardOrder.isPresent() && stageOrder(job, plan.getStagesById()) > hardOrder.getAsInt()) {
                continue;
            }
            readyJobs.add(job);
        }

        readyJobs.sort(
            Comparator
                .comparingLong(Scheduler::timeoutMillis)
                .thenComparingInt(Scheduler::negatePriority)
                .thenComparing(Job::getId)
        );

        if (readyJobs.size() > maxWorkers) {
            return new ArrayList<>(readyJobs.subList(0, maxWorkers));
        }
        return readyJobs;
    }

    public boolean allDone(Set<String> jobIds, Map<String, JobStatus> statuses) {
        for (String jobId : jobIds) {
            if (!isTerminal(statuses.getOrDefault(jobId, JobStatus.IDLE))) {
                return false;
            }
        }
        return true;
    }

    public boolean allSucceeded(Set<String> jobIds, Map<String, JobStatus> statuses) {
        for (String jobId : jobIds) {
            if (statuses.getOrDefault(jobId, JobStatus.IDLE) != JobStatus.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    private static boolean dependenciesSatisfied(
        String jobId,
        Map<String, List<String>> dependencyMap,
        Map<String, JobStatus> statuses
    ) {
        for (String upstream : dependencyMap.getOrDefault(jobId, List.of())) {
            if (statuses.getOrDefault(upstream, JobStatus.IDLE) != JobStatus.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    private static OptionalInt firstUnfinishedHardStageOrder(
        FlowExecutionPlan plan,
        Map<String, JobStatus> statuses
    ) {
        OptionalInt result = OptionalInt.empty();
        for (Job job : plan.getJobs()) {
            JobStatus status = statuses.getOrDefault(job.getId(), JobStatus.IDLE);
            if (isTerminal(status)) {
                continue;
            }
            FlowStage stage = plan.getStagesById().get(job.getStageId());
            if (stage == null || stage.getBarrierMode() != BarrierMode.HARD) {
                continue;
            }
            int order = stage.getOrder();
            if (result.isEmpty() || order < result.getAsInt()) {
                result = OptionalInt.of(order);
            }
        }
        return result;
    }

    private static int stageOrder(Job job, Map<String, FlowStage> stagesById) {
        FlowStage stage = stagesById.get(job.getStageId());
        return stage == null ? Integer.MAX_VALUE : stage.getOrder();
    }

    private static boolean isTerminal(JobStatus status) {
        return status == JobStatus.SUCCESS
            || status == JobStatus.FAILED
            || status == JobStatus.CANCELLED
            || status == JobStatus.TIMEOUT;
    }

    private static long timeoutMillis(Job job) {
        JobConfig config = job.getConfig();
        if (config == null) {
            return Duration.ofSeconds(60).toMillis();
        }
        Duration timeout = config.getTimeout();
        return timeout == null ? Duration.ofSeconds(60).toMillis() : timeout.toMillis();
    }

    private static int negatePriority(Job job) {
        JobConfig config = job.getConfig();
        return config == null ? 0 : -config.getPriority();
    }
}
