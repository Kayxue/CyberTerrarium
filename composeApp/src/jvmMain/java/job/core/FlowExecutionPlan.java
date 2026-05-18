package job.core;

import job.model.Job;
import job.model.stage.FlowStage;

import java.util.List;
import java.util.Map;

public class FlowExecutionPlan {
    private final String flowId;
    private final List<Job> jobs;
    private final Map<String, Job> jobsById;
    private final Map<String, List<String>> dependencyMap;
    private final Map<String, FlowStage> stagesById;

    public FlowExecutionPlan(
        String flowId,
        List<Job> jobs,
        Map<String, Job> jobsById,
        Map<String, List<String>> dependencyMap,
        Map<String, FlowStage> stagesById
    ) {
        this.flowId = flowId;
        this.jobs = jobs;
        this.jobsById = jobsById;
        this.dependencyMap = dependencyMap;
        this.stagesById = stagesById;
    }

    public String getFlowId() {
        return flowId;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public Map<String, Job> getJobsById() {
        return jobsById;
    }

    public Map<String, List<String>> getDependencyMap() {
        return dependencyMap;
    }

    public Map<String, FlowStage> getStagesById() {
        return stagesById;
    }
}

