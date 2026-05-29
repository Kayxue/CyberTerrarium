package job.controller;

import job.model.Job;
import job.model.JobDependency;
import job.model.flow.FlowJobLink;
import job.model.result.FlowRun;
import job.model.script.ScriptLanguage;
import job.model.stage.BarrierMode;
import job.model.stage.FlowStage;
import job.model.stage.StageFailMode;

import java.util.List;

public interface IJobController {
    List<Job> listJobs();
    List<FlowJobLink> listFlowJobs(String flowId);
    List<FlowJobLink> listAllFlowJobs();
    List<FlowStage> listAllFlowStages();
    List<JobDependency> listJobDependenciesByJobIds(List<String> jobIds);
    List<String> listFlowIds();
    List<FlowRun> listFlowRuns();
    List<FlowStage> listFlowStages(String flowId);
    String createJobForFlow(
        String flowId,
        String stageId,
        String title,
        String description,
        ScriptLanguage language,
        String scriptContent,
        int position
    );
    void createOrUpdateFlowStage(
        String flowId,
        String stageId,
        String displayName,
        int order,
        BarrierMode barrierMode,
        StageFailMode failMode
    );
    String createFlow(String flowName);
    void deleteFlow(String flowId);
    void deleteJob(String jobId);
    void updateJob(String jobId, String title, String description, String stageId, int order, boolean enabled);
    void updateFlowJobStageRelativePosition(String flowId, String jobId, double stageRelativeX, double stageRelativeY);
    void updateFlowStageWidth(String stageId, double stageWidth);
    void updateJobDependencyControlPoint(String jobId, String upstreamJobId, double bendX, double bendY);
    void saveJobDependency(String jobId, String upstreamJobId);
    void deleteJobDependency(String jobId, String upstreamJobId);
    void deleteFlowStage(String stageId);
    FlowRun runFlow(String flowId, int maxWorkers);
    int suggestNextPosition(String flowId);
}
