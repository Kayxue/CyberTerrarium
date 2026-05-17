package job.repository;

import job.model.JobDependency;

import java.util.List;

public interface IJobDependencyRepository {
    void replaceDependencies(String jobId, List<JobDependency> dependencies);
    List<JobDependency> findByJobId(String jobId);
    void deleteByJobId(String jobId);
}

