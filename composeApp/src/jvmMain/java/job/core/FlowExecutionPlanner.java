package job.core;

import job.model.Job;
import job.model.JobDependency;
import job.model.flow.FlowJobLink;
import job.model.stage.FlowStage;
import job.repository.IFlowJobRepository;
import job.repository.IFlowStageRepository;
import job.repository.IJobDependencyRepository;
import job.repository.IJobRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowExecutionPlanner {
    private final IFlowJobRepository flowJobRepository;
    private final IJobRepository jobRepository;
    private final IJobDependencyRepository jobDependencyRepository;
    private final IFlowStageRepository flowStageRepository;

    public FlowExecutionPlanner(
        IFlowJobRepository flowJobRepository,
        IJobRepository jobRepository,
        IJobDependencyRepository jobDependencyRepository,
        IFlowStageRepository flowStageRepository
    ) {
        this.flowJobRepository = flowJobRepository;
        this.jobRepository = jobRepository;
        this.jobDependencyRepository = jobDependencyRepository;
        this.flowStageRepository = flowStageRepository;
    }

    public FlowExecutionPlan plan(String flowId) {
        List<FlowJobLink> links = new ArrayList<>(flowJobRepository.findManyByFlowId(flowId));
        links.sort(Comparator.comparingInt(FlowJobLink::getPosition));

        List<String> requestedJobIds = new ArrayList<>();
        for (FlowJobLink link : links) {
            requestedJobIds.add(link.getJobId());
        }
        List<Job> fetchedJobs = jobRepository.findManyByIds(requestedJobIds);
        Map<String, Job> fetchedById = new HashMap<>();
        for (Job job : fetchedJobs) {
            fetchedById.put(job.getId(), job);
        }

        Map<String, Job> jobsById = new LinkedHashMap<>();
        List<Job> jobs = new ArrayList<>();
        Map<String, List<String>> dependencyMap = new HashMap<>();
        for (FlowJobLink link : links) {
            Job job = fetchedById.get(link.getJobId());
            if (job == null || jobsById.containsKey(job.getId())) {
                continue;
            }
            jobsById.put(job.getId(), job);
            jobs.add(job);
            dependencyMap.put(job.getId(), new ArrayList<>());
        }

        List<String> includedJobIds = new ArrayList<>(jobsById.keySet());
        Set<String> includedJobIdSet = jobsById.keySet();
        for (JobDependency dependency : jobDependencyRepository.findManyByJobIds(includedJobIds)) {
            String jobId = dependency.getJobId();
            String upstream = dependency.getUpstreamJobId();
            if (includedJobIdSet.contains(jobId) && includedJobIdSet.contains(upstream)) {
                // add upstream to the dependencyMap of jobId,
                // if the jobId does not initialize in the dependencyMap, then initialize it
                dependencyMap.computeIfAbsent(jobId, _k -> new ArrayList<>()).add(upstream);
            }
        }

        Map<String, FlowStage> stagesById = new HashMap<>();
        for (FlowStage stage : flowStageRepository.findManyByFlowId(flowId)) {
            stagesById.put(stage.getId(), stage);
        }

        checkCycles(jobsById.keySet(), dependencyMap);

        return new FlowExecutionPlan(flowId, jobs, jobsById, dependencyMap, stagesById);
    }

    private static void checkCycles(Set<String> nodes, Map<String, List<String>> dependencyMap) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String node : nodes) {
            if (!visited.contains(node)) {
                dfs(node, dependencyMap, visiting, visited);
            }
        }
    }

    private static void dfs(
        String node,
        Map<String, List<String>> dependencyMap,
        Set<String> visiting,
        Set<String> visited
    ) {
        if (visiting.contains(node)) {
            throw new IllegalStateException("Cycle detected in flow dependencies at job: " + node);
        }
        if (visited.contains(node)) {
            return;
        }
        visiting.add(node);
        for (String upstream : dependencyMap.getOrDefault(node, List.of())) {
            dfs(upstream, dependencyMap, visiting, visited);
        }
        visiting.remove(node);
        visited.add(node);
    }
}
