package job.repository;

import db.DatabaseFactory;
import job.model.trigger.IntervalTrigger;
import job.model.trigger.FlowTrigger;
import job.model.trigger.ManualTrigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlowTriggerRepository implements IFlowTriggerRepository {
    private final DatabaseFactory databaseFactory;

    public FlowTriggerRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public Optional<FlowTrigger> findOneById(String jobId) {
        String sql = "SELECT trigger_type, interval_ms FROM flow_trigger WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTrigger(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one job trigger: " + jobId, e);
        }
    }

    @Override
    public List<FlowTrigger> findAll() {
        String sql = "SELECT trigger_type, interval_ms FROM flow_trigger";
        List<FlowTrigger> triggers = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                triggers.add(mapTrigger(rs));
            }
            return triggers;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all job triggers", e);
        }
    }

    @Override
    public void save(String jobId, FlowTrigger trigger) {
        String sql = """
            INSERT INTO flow_trigger(job_id, trigger_type, interval_ms)
            VALUES (?, ?, ?)
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
    public void updateOneById(String jobId, FlowTrigger trigger) {
        String sql = """
            UPDATE flow_trigger
            SET trigger_type = ?, interval_ms = ?
            WHERE job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (trigger instanceof IntervalTrigger intervalTrigger) {
                ps.setString(1, "INTERVAL");
                Duration duration = intervalTrigger.getDuration();
                ps.setLong(2, duration == null ? 60000L : duration.toMillis());
            } else {
                ps.setString(1, "MANUAL");
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update one job trigger: " + jobId, e);
        }
    }

    @Override
    public void deleteOneById(String jobId) {
        String sql = "DELETE FROM flow_trigger WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one job trigger: " + jobId, e);
        }
    }

    private static FlowTrigger mapTrigger(ResultSet rs) throws SQLException {
        String type = rs.getString("trigger_type");
        if ("INTERVAL".equalsIgnoreCase(type)) {
            long interval = rs.getLong("interval_ms");
            return new IntervalTrigger(Duration.ofMillis(interval));
        }
        return new ManualTrigger();
    }
}
