package job.controller;

import db.DatabaseFactory;
import job.core.Dispatcher;
import job.core.Executor;
import job.core.FlowExecutionPlanner;
import job.core.Scheduler;
import job.model.Job;
import job.model.JobConfig;
import job.model.JobDependency;
import job.model.flow.FlowJobLink;
import job.model.result.FlowRun;
import job.model.script.JobScript;
import job.model.script.ScriptLanguage;
import job.model.stage.BarrierMode;
import job.model.stage.FlowStage;
import job.model.stage.StageFailMode;
import job.model.trigger.ManualTrigger;
import job.repository.FlowJobRepository;
import job.repository.FlowRunRepository;
import job.repository.FlowStageRepository;
import job.repository.FlowTriggerRepository;
import job.repository.IFlowJobRepository;
import job.repository.IFlowRunRepository;
import job.repository.IFlowStageRepository;
import job.repository.IFlowTriggerRepository;
import job.repository.IJobConfigRepository;
import job.repository.IJobDependencyRepository;
import job.repository.IJobRepository;
import job.repository.JobConfigRepository;
import job.repository.JobDependencyRepository;
import job.repository.JobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JobController implements IJobController {
    private final IJobRepository jobRepository;
    private final IJobConfigRepository jobConfigRepository;
    private final IJobDependencyRepository jobDependencyRepository;
    private final IFlowTriggerRepository flowTriggerRepository;
    private final IFlowJobRepository flowJobRepository;
    private final IFlowStageRepository flowStageRepository;
    private final IFlowRunRepository flowRunRepository;
    private final Executor executor;

    public JobController(
        IJobRepository jobRepository,
        IJobConfigRepository jobConfigRepository,
        IJobDependencyRepository jobDependencyRepository,
        IFlowTriggerRepository flowTriggerRepository,
        IFlowJobRepository flowJobRepository,
        IFlowStageRepository flowStageRepository,
        IFlowRunRepository flowRunRepository,
        Executor executor
    ) {
        this.jobRepository = jobRepository;
        this.jobConfigRepository = jobConfigRepository;
        this.jobDependencyRepository = jobDependencyRepository;
        this.flowTriggerRepository = flowTriggerRepository;
        this.flowJobRepository = flowJobRepository;
        this.flowStageRepository = flowStageRepository;
        this.flowRunRepository = flowRunRepository;
        this.executor = executor;
    }

    public static JobController createDefault() {
        DatabaseFactory db = DatabaseFactory.getInstance();
        IJobRepository jobRepository = new JobRepository(db);
        IJobConfigRepository configRepository = new JobConfigRepository(db);
        IFlowTriggerRepository triggerRepository = new FlowTriggerRepository(db);
        IJobDependencyRepository dependencyRepository = new JobDependencyRepository(db);
        IFlowJobRepository flowJobRepository = new FlowJobRepository(db);
        IFlowStageRepository flowStageRepository = new FlowStageRepository(db);
        IFlowRunRepository flowRunRepository = new FlowRunRepository(db);

        FlowExecutionPlanner planner = new FlowExecutionPlanner(
            flowJobRepository,
            jobRepository,
            dependencyRepository,
            flowStageRepository
        );
        Scheduler scheduler = new Scheduler();
        Dispatcher dispatcher = new Dispatcher();
        Executor executor = new Executor(planner, scheduler, dispatcher, flowRunRepository);
        return new JobController(
            jobRepository,
            configRepository,
            dependencyRepository,
            triggerRepository,
            flowJobRepository,
            flowStageRepository,
            flowRunRepository,
            executor
        );
    }

    @Override
    public List<Job> listJobs() {
        return jobRepository.findAll();
    }

    @Override
    public List<FlowJobLink> listFlowJobs(String flowId) {
        return flowJobRepository.findManyByFlowId(flowId);
    }

    @Override
    public List<FlowJobLink> listAllFlowJobs() {
        return flowJobRepository.findAll();
    }

    @Override
    public List<FlowStage> listAllFlowStages() {
        return flowStageRepository.findAll();
    }

    @Override
    public List<JobDependency> listJobDependenciesByJobIds(List<String> jobIds) {
        return jobDependencyRepository.findManyByJobIds(jobIds);
    }

    @Override
    public List<String> listFlowIds() {
        Set<String> flowIds = new LinkedHashSet<>();
        for (FlowJobLink link : flowJobRepository.findAll()) {
            if (link.getFlowId() != null && !link.getFlowId().isBlank()) {
                flowIds.add(link.getFlowId());
            }
        }
        for (FlowStage stage : flowStageRepository.findAll()) {
            if (stage.getFlowId() != null && !stage.getFlowId().isBlank()) {
                flowIds.add(stage.getFlowId());
            }
        }
        for (FlowRun run : flowRunRepository.findAll()) {
            if (run.getFlowId() != null && !run.getFlowId().isBlank()) {
                flowIds.add(run.getFlowId());
            }
        }
        return flowIds.stream().sorted().toList();
    }

    @Override
    public List<FlowRun> listFlowRuns() {
        return flowRunRepository.findAll();
    }

    @Override
    public List<FlowStage> listFlowStages(String flowId) {
        return flowStageRepository.findManyByFlowId(flowId);
    }

    @Override
    public String createJobForFlow(
        String flowId,
        String stageId,
        String title,
        String description,
        ScriptLanguage language,
        String scriptContent,
        int position
    ) {
        String normalizedFlowId = normalize(flowId, "demo-flow");
        String normalizedStageId = normalize(stageId, "default-stage");
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setTitle(normalize(title, "Untitled Job"));
        job.setDescription(description == null ? "" : description);
        job.setStageId(normalizedStageId);
        job.setScript(new JobScript(language, scriptContent == null ? "" : scriptContent));
        job.setConfig(new JobConfig());
        job.setTrigger(new ManualTrigger());
        job.setEnabled(true);
        job.setOrder(position);

        jobRepository.save(job);
        jobConfigRepository.save(job.getId(), job.getConfig());
        flowTriggerRepository.save(job.getId(), job.getTrigger());
        flowJobRepository.save(new FlowJobLink(normalizedFlowId, job.getId(), position));
        return job.getId();
    }

    @Override
    public void createOrUpdateFlowStage(
        String flowId,
        String stageId,
        String displayName,
        int order,
        BarrierMode barrierMode,
        StageFailMode failMode
    ) {
        FlowStage stage = new FlowStage();
        stage.setId(normalize(stageId, "default-stage"));
        stage.setFlowId(normalize(flowId, "demo-flow"));
        stage.setDisplayName(normalize(displayName, stage.getId()));
        stage.setOrder(order);
        stage.setBarrierMode(barrierMode == null ? BarrierMode.SOFT : barrierMode);
        stage.setFailMode(failMode == null ? StageFailMode.STOP : failMode);
        FlowStage existing = flowStageRepository.findOneById(stage.getId()).orElse(null);
        if (existing != null) {
            stage.setStageWidth(existing.getStageWidth());
            flowStageRepository.updateOneById(stage.getId(), stage);
        } else {
            stage.setStageWidth(-1d);
            flowStageRepository.save(stage);
        }
    }

    @Override
    public String createFlow(String flowName) {
        String normalizedFlowName = normalize(flowName, "Untitled Flow");
        String normalizedFlowId = generateUniqueFlowId();
        String normalizedStageId = generateUniqueStageId(normalizedFlowId);
        FlowStage stage = new FlowStage();
        stage.setId(normalizedStageId);
        stage.setFlowId(normalizedFlowId);
        stage.setDisplayName(normalizedFlowName);
        stage.setOrder(0);
        stage.setBarrierMode(BarrierMode.SOFT);
        stage.setFailMode(StageFailMode.STOP);
        stage.setStageWidth(-1d);
        flowStageRepository.save(stage);
        return normalizedFlowId;
    }

    @Override
    public void deleteFlow(String flowId) {
        for (FlowJobLink link : flowJobRepository.findManyByFlowId(flowId)) {
            flowJobRepository.deleteOneById(link.getFlowId(), link.getJobId());
        }
        for (FlowStage stage : flowStageRepository.findManyByFlowId(flowId)) {
            flowStageRepository.deleteOneById(stage.getId());
        }
        for (FlowRun run : flowRunRepository.findAll()) {
            if (flowId.equals(run.getFlowId())) {
                flowRunRepository.deleteOneById(run.getId());
            }
        }
    }

    @Override
    public void deleteJob(String jobId) {
        for (FlowJobLink link : flowJobRepository.findAll()) {
            if (jobId.equals(link.getJobId())) {
                flowJobRepository.deleteOneById(link.getFlowId(), link.getJobId());
            }
        }
        for (JobDependency dependency : jobDependencyRepository.findAll()) {
            if (jobId.equals(dependency.getJobId()) || jobId.equals(dependency.getUpstreamJobId())) {
                jobDependencyRepository.deleteOneById(dependency.getJobId(), dependency.getUpstreamJobId());
            }
        }
        flowTriggerRepository.deleteOneById(jobId);
        jobConfigRepository.deleteOneById(jobId);
        jobRepository.deleteOneById(jobId);
    }

    @Override
    public void updateJob(String jobId, String title, String description, String stageId, int order, boolean enabled) {
        Job existing = jobRepository.findOneById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        existing.setTitle(normalize(title, existing.getTitle()));
        existing.setDescription(description == null ? "" : description);
        existing.setStageId(normalize(stageId, existing.getStageId()));
        existing.setOrder(order);
        existing.setEnabled(enabled);
        jobRepository.updateOneById(jobId, existing);
        FlowStage stage = flowStageRepository.findOneById(existing.getStageId())
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + existing.getStageId()));
        cleanupInvalidDependenciesForFlow(stage.getFlowId());
    }

    @Override
    public void updateFlowJobStageRelativePosition(String flowId, String jobId, double stageRelativeX, double stageRelativeY) {
        FlowJobLink existing = flowJobRepository.findOneById(flowId, jobId)
            .orElseThrow(() -> new IllegalArgumentException("Flow job link not found: " + flowId + "/" + jobId));
        double normalizedX = Math.max(0d, Math.min(1d, stageRelativeX));
        double normalizedY = Math.max(0d, Math.min(1d, stageRelativeY));
        existing.setStageRelativeX(normalizedX);
        existing.setStageRelativeY(normalizedY);
        flowJobRepository.updateOneById(flowId, jobId, existing);
    }

    @Override
    public void updateFlowStageWidth(String stageId, double stageWidth) {
        FlowStage stage = flowStageRepository.findOneById(stageId)
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        double normalized = stageWidth <= 0d ? -1d : stageWidth;
        stage.setStageWidth(normalized);
        flowStageRepository.updateOneById(stageId, stage);
    }

    @Override
    public void updateJobDependencyControlPoint(String jobId, String upstreamJobId, double bendX, double bendY) {
        double normalizedBendX = bendX < 0d ? -1d : bendX;
        double normalizedBendY = bendY < 0d ? -1d : bendY;
        jobDependencyRepository.updateControlPoint(jobId, upstreamJobId, normalizedBendX, normalizedBendY);
    }

    @Override
    public void saveJobDependency(String jobId, String upstreamJobId) {
        String normalizedJobId = normalize(jobId, "");
        String normalizedUpstream = normalize(upstreamJobId, "");
        if (normalizedJobId.isEmpty() || normalizedUpstream.isEmpty()) {
            return;
        }
        if (normalizedJobId.equals(normalizedUpstream)) {
            throw new IllegalArgumentException("Dependency cannot point to itself");
        }
        Job currentJob = jobRepository.findOneById(normalizedJobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + normalizedJobId));
        Job upstreamJob = jobRepository.findOneById(normalizedUpstream)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + normalizedUpstream));
        FlowStage currentStage = flowStageRepository.findOneById(currentJob.getStageId())
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + currentJob.getStageId()));
        FlowStage upstreamStage = flowStageRepository.findOneById(upstreamJob.getStageId())
            .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + upstreamJob.getStageId()));

        if (!currentStage.getFlowId().equals(upstreamStage.getFlowId())) {
            throw new IllegalArgumentException("Dependency must stay within the same flow");
        }
        if (upstreamStage.getOrder() > currentStage.getOrder()) {
            throw new IllegalArgumentException("Dependency cannot link to a later stage");
        }
        validateOrderDependency(currentJob, upstreamJob);

        if (jobDependencyRepository.findOneById(normalizedJobId, normalizedUpstream).isPresent()) {
            return;
        }
        if (willCreateCycle(normalizedJobId, normalizedUpstream)) {
            throw new IllegalArgumentException("Dependency would create a cycle");
        }
        JobDependency dependency = new JobDependency();
        dependency.setJobId(normalizedJobId);
        dependency.setUpstreamJobId(normalizedUpstream);
        jobDependencyRepository.save(normalizedJobId, dependency);
    }

    @Override
    public void deleteJobDependency(String jobId, String upstreamJobId) {
        jobDependencyRepository.deleteOneById(jobId, upstreamJobId);
    }

    @Override
    public void deleteFlowStage(String stageId) {
        flowStageRepository.deleteOneById(stageId);
    }

    @Override
    public FlowRun runFlow(String flowId, int maxWorkers) {
        int workers = Math.max(1, maxWorkers);
        return executor.execute(normalize(flowId, "demo-flow"), workers);
    }

    @Override
    public int suggestNextPosition(String flowId) {
        return listFlowJobs(flowId).stream()
            .map(FlowJobLink::getPosition)
            .max(Comparator.naturalOrder())
            .orElse(-1) + 1;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String generateUniqueFlowId() {
        Set<String> existingFlowIds = new LinkedHashSet<>(listFlowIds());
        String candidate = "flow-" + UUID.randomUUID().toString().substring(0, 8);
        while (existingFlowIds.contains(candidate)) {
            candidate = "flow-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return candidate;
    }

    private String generateUniqueStageId(String flowId) {
        String base = flowId + "-stage-1";
        String candidate = base;
        int suffix = 2;
        while (flowStageRepository.findOneById(candidate).isPresent()) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private void validateOrderDependency(Job currentJob, Job upstreamJob) {
        if (!currentJob.getStageId().equals(upstreamJob.getStageId())) {
            return;
        }
        if (upstreamJob.getOrder() > currentJob.getOrder()) {
            throw new IllegalArgumentException("Dependency cannot point from higher order to lower order");
        }
    }

    private void cleanupInvalidDependenciesForFlow(String flowId) {
        if (flowId == null || flowId.isBlank()) {
            return;
        }
        List<FlowStage> stages = flowStageRepository.findManyByFlowId(flowId);
        java.util.Map<String, FlowStage> stageById = new java.util.HashMap<>();
        for (FlowStage stage : stages) {
            stageById.put(stage.getId(), stage);
        }

        java.util.Map<String, Job> jobById = new java.util.HashMap<>();
        for (Job job : jobRepository.findAll()) {
            if (stageById.containsKey(job.getStageId())) {
                jobById.put(job.getId(), job);
            }
        }

        List<JobDependency> toDelete = new ArrayList<>();
        for (JobDependency dependency : jobDependencyRepository.findAll()) {
            Job downstream = jobById.get(dependency.getJobId());
            Job upstream = jobById.get(dependency.getUpstreamJobId());
            if (downstream == null || upstream == null) {
                continue;
            }
            FlowStage downstreamStage = stageById.get(downstream.getStageId());
            FlowStage upstreamStage = stageById.get(upstream.getStageId());
            if (downstreamStage == null || upstreamStage == null) {
                continue;
            }
            boolean invalidByStage = upstreamStage.getOrder() > downstreamStage.getOrder();
            boolean invalidByOrder = upstreamStage.getId().equals(downstreamStage.getId()) &&
                upstream.getOrder() > downstream.getOrder();
            if (invalidByStage || invalidByOrder) {
                toDelete.add(dependency);
            }
        }
        for (JobDependency dependency : toDelete) {
            jobDependencyRepository.deleteOneById(dependency.getJobId(), dependency.getUpstreamJobId());
        }
    }

    private boolean willCreateCycle(String jobId, String upstreamJobId) {
        // Existing edges represent upstream -> job.
        java.util.Map<String, java.util.Set<String>> downstreamByUpstream = new java.util.HashMap<>();
        for (JobDependency dependency : jobDependencyRepository.findAll()) {
            downstreamByUpstream
                .computeIfAbsent(dependency.getUpstreamJobId(), ignored -> new java.util.HashSet<>())
                .add(dependency.getJobId());
        }

        // If upstream is reachable from job, adding upstream -> job creates a cycle.
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<String> visited = new java.util.HashSet<>();
        queue.add(jobId);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (current.equals(upstreamJobId)) {
                return true;
            }
            for (String next : downstreamByUpstream.getOrDefault(current, java.util.Set.of())) {
                if (!visited.contains(next)) {
                    queue.addLast(next);
                }
            }
        }
        return false;
    }
}
