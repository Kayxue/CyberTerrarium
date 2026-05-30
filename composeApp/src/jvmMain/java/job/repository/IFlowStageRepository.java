package job.repository;

import job.model.stage.FlowStage;

import java.util.List;
import java.util.Optional;

public interface IFlowStageRepository {
    Optional<FlowStage> findOneById(String stageId);
    List<FlowStage> findManyByFlowId(String flowId);
    List<FlowStage> findAll();
    void save(FlowStage stage);
    void updateOneById(String stageId, FlowStage stage);
    void deleteOneById(String stageId);
}
