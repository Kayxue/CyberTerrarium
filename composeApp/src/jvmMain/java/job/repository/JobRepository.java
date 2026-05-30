package job.repository;

import db.DatabaseFactory;
import job.model.Job;
import job.model.script.JobScript;
import job.model.script.ScriptLanguage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JobRepository implements IJobRepository {
    private final DatabaseFactory databaseFactory;

    public JobRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }


    @Override
    public Optional<Job> findOneById(String id) {
        String sql = """
            SELECT id, stage_id, order_no, title, description, script_language, script_content, enabled
            FROM job
            WHERE id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapJob(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one job: " + id, e);
        }
    }

    @Override
    public List<Job> findManyByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalized.add(id);
            }
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized.size(), "?"));
        String sql = """
            SELECT id, stage_id, order_no, title, description, script_language, script_content, enabled
            FROM job
            WHERE id IN (%s)
            """.formatted(placeholders);
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String id : normalized) {
                ps.setString(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(mapJob(rs));
                }
            }
            return jobs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find many jobs by ids", e);
        }
    }

    @Override
    public List<Job> findAll() {
        String sql = """
            SELECT id, stage_id, order_no, title, description, script_language, script_content, enabled
            FROM job
            ORDER BY created_at DESC
            """;
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                jobs.add(mapJob(rs));
            }
            return jobs;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list jobs", e);
        }
    }

    @Override
    public void save(Job job) {
        String sql = """
            INSERT INTO job(id, stage_id, order_no, title, description, script_language, script_content, enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nullToEmpty(job.getId()));
            ps.setString(2, nullToEmpty(job.getStageId()));
            ps.setInt(3, job.getOrder());
            ps.setString(4, nullToEmpty(job.getTitle()));
            ps.setString(5, nullToEmpty(job.getDescription()));
            JobScript script = job.getScript() == null ? new JobScript() : job.getScript();
            ps.setString(6, script.getLanguage().name());
            ps.setString(7, nullToEmpty(script.getContent()));
            ps.setInt(8, job.isEnabled() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job: " + job.getId(), e);
        }
    }

    @Override
    public void updateOneById(String id, Job job) {
        String sql = """
            UPDATE job
            SET stage_id = ?, order_no = ?, title = ?, description = ?, script_language = ?, script_content = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            JobScript script = job.getScript() == null ? new JobScript() : job.getScript();
            ps.setString(1, nullToEmpty(job.getStageId()));
            ps.setInt(2, job.getOrder());
            ps.setString(3, nullToEmpty(job.getTitle()));
            ps.setString(4, nullToEmpty(job.getDescription()));
            ps.setString(5, script.getLanguage().name());
            ps.setString(6, nullToEmpty(script.getContent()));
            ps.setInt(7, job.isEnabled() ? 1 : 0);
            ps.setString(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update job: " + id, e);
        }
    }

    @Override
    public void deleteOneById(String id) {
        String sql = "DELETE FROM job WHERE id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete one job: " + id, e);
        }
    }

    private static Job mapJob(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setId(rs.getString("id"));
        job.setStageId(rs.getString("stage_id"));
        job.setOrder(rs.getInt("order_no"));
        job.setTitle(rs.getString("title"));
        job.setDescription(rs.getString("description"));
        ScriptLanguage language = ScriptLanguage.valueOf(rs.getString("script_language"));
        String content = rs.getString("script_content");
        job.setScript(new JobScript(language, content));
        job.setEnabled(rs.getInt("enabled") == 1);
        return job;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
