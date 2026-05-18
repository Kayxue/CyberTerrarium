package job.repository;

import job.model.Job;

import java.util.List;
import java.util.Optional;

public interface IJobRepository {
    Optional<Job> findOneById(String id);
    List<Job> findManyByIds(List<String> ids);
    List<Job> findAll();
    void save(Job job);
    void updateOneById(String id, Job job);
    void deleteOneById(String id);
}
