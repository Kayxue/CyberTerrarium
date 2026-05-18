package job.repository;

import db.DatabaseFactory;
import job.model.stage.BarrierMode;
import job.model.stage.FlowStage;
import job.model.stage.StageFailMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlowStageRepository implements IFlowStageRepository {
    private final DatabaseFactory databaseFactory;

    public FlowStageRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }


    @Override
    public Optional<FlowStage> findOneById(String stageId) {
        String sql = """
            SELECT id, flow_id, display_name, order_no, barrier_mode, fail_mode
            FROM flow_stage
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
            throw new RuntimeException("Failed to find one job stage: " + stageId, e);
        }
    }

    @Override
    public List<FlowStage> findManyByFlowId(String flowId) {
        String sql = """
            SELECT id, flow_id, display_name, order_no, barrier_mode, fail_mode
            FROM flow_stage
            WHERE flow_id = ?
            ORDER BY order_no ASC, id ASC
            """;
        List<FlowStage> stages = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flowId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stages.add(mapStage(rs));
                }
            }
            return stages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flow stages by flowId: " + flowId, e);
        }
    }

    @Override
    public List<FlowStage> findAll() {
        String sql = """
            SELECT id, flow_id, display_name, order_no, barrier_mode, fail_mode
            FROM flow_stage
            ORDER BY order_no ASC, id ASC
            """;
        List<FlowStage> stages = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stages.add(mapStage(rs));
            }
            return stages;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all job stages", e);
        }
    }

    @Override
    public void save(FlowStage stage) {
        String sql = """
            INSERT INTO flow_stage(id, flow_id, display_name, order_no, barrier_mode, fail_mode)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stage.getId());
            ps.setString(2, stage.getFlowId());
            ps.setString(3, stage.getDisplayName());
            ps.setInt(4, stage.getOrder());
            ps.setString(5, stage.getBarrierMode().name());
            ps.setString(6, stage.getFailMode().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job stage: " + stage.getId(), e);
        }
    }

    @Override
    public void updateOneById(String stageId, FlowStage stage) {
        String sql = """
            UPDATE flow_stage
            SET flow_id = ?, display_name = ?, order_no = ?, barrier_mode = ?, fail_mode = ?
            WHERE id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stage.getFlowId());
            ps.setString(2, stage.getDisplayName());
            ps.setInt(3, stage.getOrder());
            ps.setString(4, stage.getBarrierMode().name());
            ps.setString(5, stage.getFailMode().name());
            ps.setString(6, stageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update one job stage: " + stageId, e);
        }
    }

    @Override
    public void deleteOneById(String stageId) {
        String sql = "DELETE FROM flow_stage WHERE id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one job stage: " + stageId, e);
        }
    }

    private static FlowStage mapStage(ResultSet rs) throws SQLException {
        FlowStage stage = new FlowStage();
        stage.setId(rs.getString("id"));
        stage.setFlowId(rs.getString("flow_id"));
        stage.setDisplayName(rs.getString("display_name"));
        stage.setOrder(rs.getInt("order_no"));
        stage.setBarrierMode(BarrierMode.valueOf(rs.getString("barrier_mode")));
        stage.setFailMode(StageFailMode.valueOf(rs.getString("fail_mode")));
        return stage;
    }
}
