package job.repository;

import db.DatabaseFactory;
import job.model.flow.FlowJobLink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlowJobRepository implements IFlowJobRepository {
    private final DatabaseFactory databaseFactory;

    public FlowJobRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public Optional<FlowJobLink> findOneById(String flowId, String jobId) {
        String sql = """
            SELECT flow_id, job_id, position
            FROM jobs_to_flows
            WHERE flow_id = ? AND job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flowId);
            ps.setString(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapLink(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one flow-job link: " + flowId + "/" + jobId, e);
        }
    }

    @Override
    public List<FlowJobLink> findManyByFlowId(String flowId) {
        String sql = """
            SELECT flow_id, job_id, position
            FROM jobs_to_flows
            WHERE flow_id = ?
            ORDER BY position ASC, job_id ASC
            """;
        List<FlowJobLink> links = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flowId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    links.add(mapLink(rs));
                }
            }
            return links;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flow-job links for flow: " + flowId, e);
        }
    }

    @Override
    public List<FlowJobLink> findAll() {
        String sql = """
            SELECT flow_id, job_id, position
            FROM jobs_to_flows
            ORDER BY flow_id ASC, position ASC, job_id ASC
            """;
        List<FlowJobLink> links = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                links.add(mapLink(rs));
            }
            return links;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all flow-job links", e);
        }
    }

    @Override
    public void save(FlowJobLink link) {
        String sql = """
            INSERT INTO jobs_to_flows(flow_id, job_id, position)
            VALUES (?, ?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, link.getFlowId());
            ps.setString(2, link.getJobId());
            ps.setInt(3, link.getPosition());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save flow-job link", e);
        }
    }

    @Override
    public void updateOneById(String flowId, String jobId, FlowJobLink link) {
        String sql = """
            UPDATE jobs_to_flows
            SET flow_id = ?, job_id = ?, position = ?
            WHERE flow_id = ? AND job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, link.getFlowId());
            ps.setString(2, link.getJobId());
            ps.setInt(3, link.getPosition());
            ps.setString(4, flowId);
            ps.setString(5, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update one flow-job link", e);
        }
    }

    @Override
    public void deleteOneById(String flowId, String jobId) {
        String sql = "DELETE FROM jobs_to_flows WHERE flow_id = ? AND job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flowId);
            ps.setString(2, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one flow-job link", e);
        }
    }

    private static FlowJobLink mapLink(ResultSet rs) throws SQLException {
        return new FlowJobLink(
            rs.getString("flow_id"),
            rs.getString("job_id"),
            rs.getInt("position")
        );
    }
}
