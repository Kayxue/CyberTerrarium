package job.repository;

import job.model.stage.JobStage;

import java.util.List;
import java.util.Optional;

public interface IJobStageRepository {
    void save(JobStage stage);
    Optional<JobStage> findById(String stageId);
    List<JobStage> findAll();
    void delete(String stageId);
}

