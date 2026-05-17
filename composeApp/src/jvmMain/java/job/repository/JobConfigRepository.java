package job.repository;

import db.DatabaseFactory;
import job.model.JobConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

public class JobConfigRepository implements IJobConfigRepository {
    private final DatabaseFactory databaseFactory;

    public JobConfigRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void save(String jobId, JobConfig config) {
        String sql = """
            INSERT INTO job_config(job_id, timeout_ms, retry_count, priority, attributes_text)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(job_id) DO UPDATE SET
                timeout_ms = excluded.timeout_ms,
                retry_count = excluded.retry_count,
                priority = excluded.priority,
                attributes_text = excluded.attributes_text
            """;
        long timeoutMs = config.getTimeout() == null ? 60000L : config.getTimeout().toMillis();
        String attributesText = RepositoryCodec.encodeAttributes(config.getAttributes());
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setLong(2, timeoutMs);
            ps.setInt(3, config.getRetry());
            ps.setInt(4, config.getPriority());
            ps.setString(5, attributesText);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job config: " + jobId, e);
        }
    }

    @Override
    public Optional<JobConfig> findByJobId(String jobId) {
        String sql = """
            SELECT timeout_ms, retry_count, priority, attributes_text
            FROM job_config
            WHERE job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                JobConfig config = new JobConfig();
                config.setTimeout(Duration.ofMillis(rs.getLong("timeout_ms")));
                config.setRetry(rs.getInt("retry_count"));
                config.setPriority(rs.getInt("priority"));
                config.setAttributes(RepositoryCodec.decodeAttributes(rs.getString("attributes_text")));
                return Optional.of(config);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find job config: " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        String sql = "DELETE FROM job_config WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job config: " + jobId, e);
        }
    }
}

