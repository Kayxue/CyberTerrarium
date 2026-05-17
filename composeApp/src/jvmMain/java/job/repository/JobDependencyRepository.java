package job.repository;

import db.DatabaseFactory;
import job.model.JobDependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JobDependencyRepository implements IJobDependencyRepository {
    private final DatabaseFactory databaseFactory;

    public JobDependencyRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void replaceDependencies(String jobId, List<JobDependency> dependencies) {
        String deleteSql = "DELETE FROM job_dependency WHERE job_id = ?";
        String insertSql = """
            INSERT INTO job_dependency(job_id, upstream_job_id)
            VALUES (?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                    deletePs.setString(1, jobId);
                    deletePs.executeUpdate();
                }
                if (dependencies != null && !dependencies.isEmpty()) {
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        for (JobDependency dependency : dependencies) {
                            insertPs.setString(1, jobId);
                            insertPs.setString(2, dependency.getUpstreamJobId());
                            insertPs.addBatch();
                        }
                        insertPs.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to replace job dependencies: " + jobId, e);
        }
    }

    @Override
    public List<JobDependency> findByJobId(String jobId) {
        String sql = """
            SELECT upstream_job_id
            FROM job_dependency
            WHERE job_id = ?
            ORDER BY upstream_job_id
            """;
        List<JobDependency> dependencies = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dependencies.add(new JobDependency(rs.getString("upstream_job_id")));
                }
            }
            return dependencies;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list job dependencies: " + jobId, e);
        }
    }

    @Override
    public void deleteByJobId(String jobId) {
        String sql = "DELETE FROM job_dependency WHERE job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job dependencies: " + jobId, e);
        }
    }
}

