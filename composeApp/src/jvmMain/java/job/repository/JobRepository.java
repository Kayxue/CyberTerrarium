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
import java.util.List;
import java.util.Optional;

public class JobRepository implements IJobRepository {
    private final DatabaseFactory databaseFactory;

    public JobRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public void save(Job job) {
        String sql = """
            INSERT INTO job(id, stage_id, title, description, script_language, script_content, enabled, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                stage_id = excluded.stage_id,
                title = excluded.title,
                description = excluded.description,
                script_language = excluded.script_language,
                script_content = excluded.script_content,
                enabled = excluded.enabled,
                updated_at = CURRENT_TIMESTAMP
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nullToEmpty(job.getId()));
            ps.setString(2, nullToEmpty(job.getStageId()));
            ps.setString(3, nullToEmpty(job.getTitle()));
            ps.setString(4, nullToEmpty(job.getDescription()));
            JobScript script = job.getScript() == null ? new JobScript() : job.getScript();
            ps.setString(5, script.getLanguage().name());
            ps.setString(6, nullToEmpty(script.getContent()));
            ps.setInt(7, job.isEnabled() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save job: " + job.getId(), e);
        }
    }

    @Override
    public Optional<Job> findById(String id) {
        String sql = """
            SELECT id, stage_id, title, description, script_language, script_content, enabled
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
            throw new RuntimeException("Failed to find job: " + id, e);
        }
    }

    @Override
    public List<Job> findAll() {
        String sql = """
            SELECT id, stage_id, title, description, script_language, script_content, enabled
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
    public void delete(String id) {
        String sql = "DELETE FROM job WHERE id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete job: " + id, e);
        }
    }

    private static Job mapJob(ResultSet rs) throws SQLException {
        Job job = new Job();
        job.setId(rs.getString("id"));
        job.setStageId(rs.getString("stage_id"));
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
