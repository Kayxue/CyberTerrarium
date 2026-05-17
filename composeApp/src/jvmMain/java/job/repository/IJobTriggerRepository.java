package job.repository;

import job.model.trigger.JobTrigger;

import java.util.Optional;

public interface IJobTriggerRepository {
    void save(String jobId, JobTrigger trigger);
    Optional<JobTrigger> findByJobId(String jobId);
    void deleteByJobId(String jobId);
}

