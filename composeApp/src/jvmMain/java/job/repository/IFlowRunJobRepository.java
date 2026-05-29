package job.repository;

import job.model.result.FlowRunJob;

import java.util.List;
import java.util.Optional;

public interface IFlowRunJobRepository {
    Optional<FlowRunJob> findOneById(long runId, String jobId);
    List<FlowRunJob> findManyByRunIds(List<Long> runIds);
    List<FlowRunJob> findManyByRunId(long runId);
    List<FlowRunJob> findAll();
    void save(FlowRunJob runJob);
    void updateOneById(long runId, String jobId, FlowRunJob runJob);
    void deleteOneById(long runId, String jobId);
    void deleteManyByRunId(long runId);
}

