package job.repository;

import job.model.Job;

import java.util.List;
import java.util.Optional;

public interface IJobRepository {
    void save(Job job);
    Optional<Job> findById(String id);
    List<Job> findAll();
    void delete(String id);
}

