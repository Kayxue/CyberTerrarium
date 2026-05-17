package job.repository;

import job.model.JobConfig;

import java.util.Optional;

public interface IJobConfigRepository {
    void save(String jobId, JobConfig config);
    Optional<JobConfig> findByJobId(String jobId);
    void deleteByJobId(String jobId);
}

