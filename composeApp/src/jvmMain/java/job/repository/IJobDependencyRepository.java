package job.repository;

import job.model.JobDependency;

import java.util.List;
import java.util.Optional;

public interface IJobDependencyRepository {
    Optional<JobDependency> findOneById(String jobId, String upstreamJobId);
    List<JobDependency> findManyByJobIds(List<String> jobIds);
    List<JobDependency> findAll();
    void save(String jobId, JobDependency dependency);
    void updateOneById(String jobId, String upstreamJobId, JobDependency dependency);
    void deleteOneById(String jobId, String upstreamJobId);
}
