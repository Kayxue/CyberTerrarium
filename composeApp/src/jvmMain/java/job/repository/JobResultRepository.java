package job.repository;

import db.DatabaseFactory;
import job.model.result.JobResult;
import job.model.result.JobStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobResultRepository implements IJobResultRepository {
    private final DatabaseFactory databaseFactory;

    public JobResultRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void save(JobResult result) {
        String sql = """
            INSERT INTO job_result(job_id, status, ended_at)
            VALUES (?, ?, ?)
            """;
        Instant endedAt = result.getEndedAt() == null ? Instant.now() : result.getEndedAt();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, result.getJobId());
            ps.setString(2, result.getStatus().name());
            ps.setString(3, Timestamp.from(endedAt).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job result: " + result.getJobId(), e);
        }
    }

    @Override
    public List<JobResult> findByJobId(String jobId) {
        String sql = """
            SELECT job_id, status, ended_at
            FROM job_result
            WHERE job_id = ?
            ORDER BY ended_at DESC
            """;
        List<JobResult> results = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JobResult result = new JobResult();
                    result.setJobId(rs.getString("job_id"));
                    result.setStatus(JobStatus.valueOf(rs.getString("status")));
                    result.setEndedAt(Timestamp.valueOf(rs.getString("ended_at")).toInstant());
                    results.add(result);
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list job results: " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        String sql = "DELETE FROM job_result WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job results: " + jobId, e);
        }
    }
}

