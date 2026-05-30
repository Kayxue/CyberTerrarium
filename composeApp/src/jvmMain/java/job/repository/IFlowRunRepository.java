package job.repository;

import job.model.result.FlowRun;

import java.util.List;
import java.util.Optional;

public interface IFlowRunRepository {
    Optional<FlowRun> findOneById(long id);
    List<FlowRun> findAll();
    void save(FlowRun run);
    void updateOneById(long id, FlowRun run);
    void deleteOneById(long id);
}
