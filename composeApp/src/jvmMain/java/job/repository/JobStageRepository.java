package job.repository;

import db.DatabaseFactory;
import job.model.stage.BarrierMode;
import job.model.stage.JobStage;
import job.model.stage.StageFailMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobStageRepository implements IJobStageRepository {
    private final DatabaseFactory databaseFactory;

    public JobStageRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void save(JobStage stage) {
        String sql = """
            INSERT INTO job_stage(id, display_name, order_no, barrier_mode, fail_mode)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                display_name = excluded.display_name,
                order_no = excluded.order_no,
                barrier_mode = excluded.barrier_mode,
                fail_mode = excluded.fail_mode
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stage.getId());
            ps.setString(2, stage.getDisplayName());
            ps.setInt(3, stage.getOrder());
            ps.setString(4, stage.getBarrierMode().name());
            ps.setString(5, stage.getFailMode().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job stage: " + stage.getId(), e);
        }
    }

    @Override
    public Optional<JobStage> findById(String stageId) {
        String sql = """
            SELECT id, display_name, order_no, barrier_mode, fail_mode
            FROM job_stage
            WHERE id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapStage(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find job stage: " + stageId, e);
        }
    }

    @Override
    public List<JobStage> findAll() {
        String sql = """
            SELECT id, display_name, order_no, barrier_mode, fail_mode
            FROM job_stage
            ORDER BY order_no ASC, id ASC
            """;
        List<JobStage> stages = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stages.add(mapStage(rs));
            }
            return stages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list job stages", e);
        }
    }

    @Override
    public void delete(String stageId) {
        String sql = "DELETE FROM job_stage WHERE id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job stage: " + stageId, e);
        }
    }

    private static JobStage mapStage(ResultSet rs) throws SQLException {
        JobStage stage = new JobStage();
        stage.setId(rs.getString("id"));
        stage.setDisplayName(rs.getString("display_name"));
        stage.setOrder(rs.getInt("order_no"));
        stage.setBarrierMode(BarrierMode.valueOf(rs.getString("barrier_mode")));
        stage.setFailMode(StageFailMode.valueOf(rs.getString("fail_mode")));
        return stage;
    }
}

