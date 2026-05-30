package job.repository;

import db.DatabaseFactory;
import job.model.JobConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobConfigRepository implements IJobConfigRepository {
    private final DatabaseFactory databaseFactory;

    public JobConfigRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public Optional<JobConfig> findOneById(String jobId) {
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
                return Optional.of(mapConfig(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one job config: " + jobId, e);
        }
    }

    @Override
    public List<JobConfig> findAll() {
        String sql = """
            SELECT timeout_ms, retry_count, priority, attributes_text
            FROM job_config
            """;
        List<JobConfig> configs = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                configs.add(mapConfig(rs));
            }
            return configs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all job config", e);
        }
    }

    @Override
    public void save(String jobId, JobConfig config) {
        long timeoutMs = config.getTimeout() == null ? 60000L : config.getTimeout().toMillis();
        String attributesText = RepositoryCodec.encodeAttributes(config.getAttributes());
        String sql = """
            INSERT INTO job_config(job_id, timeout_ms, retry_count, priority, attributes_text)
            VALUES (?, ?, ?, ?, ?)
            """;
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
    public void updateOneById(String jobId, JobConfig config) {
        String sql = """
            UPDATE job_config
            SET timeout_ms = ?, retry_count = ?, priority = ?, attributes_text = ?
            WHERE job_id = ?
            """;
        long timeoutMs = config.getTimeout() == null ? 60000L : config.getTimeout().toMillis();
        String attributesText = RepositoryCodec.encodeAttributes(config.getAttributes());
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timeoutMs);
            ps.setInt(2, config.getRetry());
            ps.setInt(3, config.getPriority());
            ps.setString(4, attributesText);
            ps.setString(5, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update one job config: " + jobId, e);
        }
    }

    @Override
    public void deleteOneById(String jobId) {
        String sql = "DELETE FROM job_config WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one job config: " + jobId, e);
        }
    }

    private static JobConfig mapConfig(ResultSet rs) throws SQLException {
        JobConfig config = new JobConfig();
        config.setTimeout(Duration.ofMillis(rs.getLong("timeout_ms")));
        config.setRetry(rs.getInt("retry_count"));
        config.setPriority(rs.getInt("priority"));
        config.setAttributes(RepositoryCodec.decodeAttributes(rs.getString("attributes_text")));
        return config;
    }
}
