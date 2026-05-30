package job.repository;

import job.model.trigger.FlowTrigger;

import java.util.List;
import java.util.Optional;

public interface IFlowTriggerRepository {
    Optional<FlowTrigger> findOneById(String jobId);
    List<FlowTrigger> findAll();
    void save(String jobId, FlowTrigger trigger);
    void updateOneById(String jobId, FlowTrigger trigger);
    void deleteOneById(String jobId);
}
