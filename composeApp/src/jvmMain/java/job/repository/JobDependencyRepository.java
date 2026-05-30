package job.repository;

import db.DatabaseFactory;
import job.model.JobDependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JobDependencyRepository implements IJobDependencyRepository {
    private final DatabaseFactory databaseFactory;

    public JobDependencyRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public Optional<JobDependency> findOneById(String jobId, String upstreamJobId) {
        String sql = """
            SELECT job_id, upstream_job_id, bend_x, bend_y
            FROM job_dependency
            WHERE job_id = ? AND upstream_job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setString(2, upstreamJobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapDependency(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find one job dependency: " + jobId + " -> " + upstreamJobId,
                    e
            );
        }
    }

    @Override
    public List<JobDependency> findAll() {
        String sql = """
            SELECT job_id, upstream_job_id, bend_x, bend_y
            FROM job_dependency
            ORDER BY job_id, upstream_job_id
            """;
        List<JobDependency> dependencies = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                dependencies.add(mapDependency(rs));
            }
            return dependencies;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all job dependencies", e);
        }
    }

    @Override
    public List<JobDependency> findManyByJobIds(List<String> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String jobId : jobIds) {
            if (jobId != null && !jobId.isBlank()) {
                normalized.add(jobId);
            }
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized.size(), "?"));
        String sql = """
            SELECT job_id, upstream_job_id, bend_x, bend_y
            FROM job_dependency
            WHERE job_id IN (%s)
            ORDER BY job_id, upstream_job_id
            """.formatted(placeholders);

        List<JobDependency> dependencies = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String jobId : normalized) {
                ps.setString(index++, jobId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dependencies.add(mapDependency(rs));
                }
            }
            return dependencies;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find many job dependencies by job ids", e);
        }
    }

    @Override
    public void save(String jobId, JobDependency dependency) {
        String sql = """
            INSERT INTO job_dependency(job_id, upstream_job_id, bend_x, bend_y)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setString(2, dependency.getUpstreamJobId());
            ps.setDouble(3, dependency.getBendX());
            ps.setDouble(4, dependency.getBendY());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job dependency: " + jobId, e);
        }
    }

    @Override
    public void updateOneById(String jobId, String upstreamJobId, JobDependency dependency) {
        String sql = """
            UPDATE job_dependency
            SET upstream_job_id = ?, bend_x = ?, bend_y = ?
            WHERE job_id = ? AND upstream_job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dependency.getUpstreamJobId());
            ps.setDouble(2, dependency.getBendX());
            ps.setDouble(3, dependency.getBendY());
            ps.setString(4, jobId);
            ps.setString(5, upstreamJobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to update one job dependency: " + jobId + " -> " + upstreamJobId,
                e
            );
        }
    }

    @Override
    public void updateControlPoint(String jobId, String upstreamJobId, double bendX, double bendY) {
        String sql = """
            UPDATE job_dependency
            SET bend_x = ?, bend_y = ?
            WHERE job_id = ? AND upstream_job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, bendX);
            ps.setDouble(2, bendY);
            ps.setString(3, jobId);
            ps.setString(4, upstreamJobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to update dependency control point: " + jobId + " -> " + upstreamJobId,
                e
            );
        }
    }

    @Override
    public void deleteOneById(String jobId, String upstreamJobId) {
        String sql = "DELETE FROM job_dependency WHERE job_id = ? AND upstream_job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId);
            ps.setString(2, upstreamJobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to delete one job dependency: " + jobId + " -> " + upstreamJobId,
                e
            );
        }
    }

    private static JobDependency mapDependency(ResultSet rs) throws SQLException {
        JobDependency dependency = new JobDependency();
        dependency.setJobId(rs.getString("job_id"));
        dependency.setUpstreamJobId(rs.getString("upstream_job_id"));
        dependency.setBendX(rs.getDouble("bend_x"));
        dependency.setBendY(rs.getDouble("bend_y"));
        return dependency;
    }
}
