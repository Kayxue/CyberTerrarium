package job.repository;

import db.DatabaseFactory;
import job.model.result.FlowRun;
import job.model.result.FlowStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlowRunRepository implements IFlowRunRepository {
    private final DatabaseFactory databaseFactory;

    public FlowRunRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }


    @Override
    public Optional<FlowRun> findOneById(long id) {
        String sql = """
            SELECT id, flow_id, status, started_at, ended_at
            FROM flow_run
            WHERE id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRun(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one flow run: " + id, e);
        }
    }

    @Override
    public List<FlowRun> findAll() {
        String sql = """
            SELECT id, flow_id, status, started_at, ended_at
            FROM flow_run
            ORDER BY ended_at DESC, id DESC
            """;
        List<FlowRun> runs = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                runs.add(mapRun(rs));
            }
            return runs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all flow runs", e);
        }
    }

    @Override
    public void save(FlowRun run) {
        String sql = """
            INSERT INTO flow_run(flow_id, status, started_at, ended_at)
            VALUES (?, ?, ?, ?)
            """;
        Instant startedAt = run.getStartedAt();
        if (startedAt == null) {
            startedAt = Instant.now();
            run.setStartedAt(startedAt);
        }
        Instant endedAt = run.getEndedAt();
        if (endedAt == null) {
            endedAt = Instant.now();
            run.setEndedAt(endedAt);
        }
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, run.getFlowId());
            ps.setString(2, run.getStatus().name());
            ps.setString(3, Timestamp.from(startedAt).toString());
            ps.setString(4, Timestamp.from(endedAt).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    run.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save flow run: " + run.getFlowId(), e);
        }
    }

    @Override
    public void updateOneById(long id, FlowRun run) {
        String sql = """
            UPDATE flow_run
            SET flow_id = ?, status = ?, started_at = ?, ended_at = ?
            WHERE id = ?
            """;
        Instant startedAt = run.getStartedAt();
        if (startedAt == null) {
            startedAt = Instant.now();
            run.setStartedAt(startedAt);
        }
        Instant endedAt = run.getEndedAt();
        if (endedAt == null) {
            endedAt = Instant.now();
            run.setEndedAt(endedAt);
        }
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, run.getFlowId());
            ps.setString(2, run.getStatus().name());
            ps.setString(3, Timestamp.from(startedAt).toString());
            ps.setString(4, Timestamp.from(endedAt).toString());
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update one flow run: " + id, e);
        }
    }

    @Override
    public void deleteOneById(long id) {
        String sql = "DELETE FROM flow_run WHERE id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one flow run: " + id, e);
        }
    }

    private static FlowRun mapRun(ResultSet rs) throws SQLException {
        FlowRun run = new FlowRun();
        run.setId(rs.getLong("id"));
        run.setFlowId(rs.getString("flow_id"));
        run.setStatus(FlowStatus.valueOf(rs.getString("status")));
        String startedAt = rs.getString("started_at");
        String endedAt = rs.getString("ended_at");
        if (startedAt != null) {
            run.setStartedAt(Timestamp.valueOf(startedAt).toInstant());
        }
        if (endedAt != null) {
            run.setEndedAt(Timestamp.valueOf(endedAt).toInstant());
        }
        return run;
    }
}
