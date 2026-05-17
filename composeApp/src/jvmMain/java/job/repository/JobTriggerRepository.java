package job.repository;

import db.DatabaseFactory;
import job.model.trigger.IntervalTrigger;
import job.model.trigger.JobTrigger;
import job.model.trigger.ManualTrigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

public class JobTriggerRepository implements IJobTriggerRepository {
    private final DatabaseFactory databaseFactory;

    public JobTriggerRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void save(String jobId, JobTrigger trigger) {
        String sql = """
            INSERT INTO job_trigger(job_id, trigger_type, interval_ms)
            VALUES (?, ?, ?)
            ON CONFLICT(job_id) DO UPDATE SET
                trigger_type = excluded.trigger_type,
                interval_ms = excluded.interval_ms
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            if (trigger instanceof IntervalTrigger intervalTrigger) {
                ps.setString(2, "INTERVAL");
                Duration duration = intervalTrigger.getDuration();
                ps.setLong(3, duration == null ? 60000L : duration.toMillis());
            } else {
                ps.setString(2, "MANUAL");
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job trigger: " + jobId, e);
        }
    }

    @Override
    public Optional<JobTrigger> findByJobId(String jobId) {
        String sql = "SELECT trigger_type, interval_ms FROM job_trigger WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String type = rs.getString("trigger_type");
                if ("INTERVAL".equalsIgnoreCase(type)) {
                    long interval = rs.getLong("interval_ms");
                    return Optional.of(new IntervalTrigger(Duration.ofMillis(interval)));
                }
                return Optional.of(new ManualTrigger());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find job trigger: " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        String sql = "DELETE FROM job_trigger WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job trigger: " + jobId, e);
        }
    }
}

