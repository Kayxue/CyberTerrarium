package job.repository;

import job.model.result.JobResult;

import java.util.List;

public interface IJobResultRepository {
    void save(JobResult result);
    List<JobResult> findByJobId(String jobId);
    void deleteByJobId(String jobId);
}

