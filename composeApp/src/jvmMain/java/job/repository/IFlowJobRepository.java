package job.repository;

import job.model.flow.FlowJobLink;

import java.util.List;
import java.util.Optional;

public interface IFlowJobRepository {
    Optional<FlowJobLink> findOneById(String flowId, String jobId);
    List<FlowJobLink> findAll();
    void save(FlowJobLink link);
    void updateOneById(String flowId, String jobId, FlowJobLink link);
    void deleteOneById(String flowId, String jobId);
    List<FlowJobLink> findManyByFlowId(String flowId);
}
