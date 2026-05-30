package job.repository;

import job.model.JobConfig;

import java.util.List;
import java.util.Optional;

public interface IJobConfigRepository {
    Optional<JobConfig> findOneById(String jobId);
    List<JobConfig> findAll();
    void save(String jobId, JobConfig config);
    void updateOneById(String jobId, JobConfig config);
    void deleteOneById(String jobId);
}
