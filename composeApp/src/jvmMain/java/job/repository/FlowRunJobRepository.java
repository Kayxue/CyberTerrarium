package job.repository;

import db.DatabaseFactory;
import job.model.result.FlowRunJob;
import job.model.result.JobStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FlowRunJobRepository implements IFlowRunJobRepository {
    private final DatabaseFactory databaseFactory;

    public FlowRunJobRepository(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
    }

    @Override
    public Optional<FlowRunJob> findOneById(long runId, String jobId) {
        String sql = """
            SELECT run_id, job_id, status, exit_code, stdout_text, stderr_text, error_message, started_at, ended_at, duration_ms
            FROM flow_run_job
            WHERE run_id = ? AND job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            ps.setString(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRunJob(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find one flow run job: " + runId + "/" + jobId, e);
        }
    }

    @Override
    public List<FlowRunJob> findManyByRunIds(List<Long> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long runId : runIds) {
            if (runId != null && runId > 0L) {
                normalized.add(runId);
            }
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(normalized.size(), "?"));
        String sql = """
            SELECT run_id, job_id, status, exit_code, stdout_text, stderr_text, error_message, started_at, ended_at, duration_ms
            FROM flow_run_job
            WHERE run_id IN (%s)
            ORDER BY run_id DESC, started_at ASC, job_id ASC
            """.formatted(placeholders);
        List<FlowRunJob> records = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Long runId : normalized) {
                ps.setLong(index++, runId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRunJob(rs));
                }
            }
            return records;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find flow run jobs by run ids", e);
        }
    }

    @Override
    public List<FlowRunJob> findManyByRunId(long runId) {
        String sql = """
            SELECT run_id, job_id, status, exit_code, stdout_text, stderr_text, error_message, started_at, ended_at, duration_ms
            FROM flow_run_job
            WHERE run_id = ?
            ORDER BY started_at ASC, job_id ASC
            """;
        List<FlowRunJob> records = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRunJob(rs));
                }
            }
            return records;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flow run jobs for run: " + runId, e);
        }
    }

    @Override
    public List<FlowRunJob> findAll() {
        String sql = """
            SELECT run_id, job_id, status, exit_code, stdout_text, stderr_text, error_message, started_at, ended_at, duration_ms
            FROM flow_run_job
            ORDER BY run_id DESC, started_at ASC, job_id ASC
            """;
        List<FlowRunJob> records = new ArrayList<>();
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(mapRunJob(rs));
            }
            return records;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list all flow run jobs", e);
        }
    }

    @Override
    public void save(FlowRunJob runJob) {
        String sql = """
            INSERT INTO flow_run_job(run_id, job_id, status, exit_code, stdout_text, stderr_text, error_message, started_at, ended_at, duration_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            writeRunJob(ps, runJob);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save flow run job: " + runJob.getRunId() + "/" + runJob.getJobId(), e);
        }
    }

    @Override
    public void updateOneById(long runId, String jobId, FlowRunJob runJob) {
        String sql = """
            UPDATE flow_run_job
            SET status = ?, exit_code = ?, stdout_text = ?, stderr_text = ?, error_message = ?, started_at = ?, ended_at = ?, duration_ms = ?
            WHERE run_id = ? AND job_id = ?
            """;
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Instant startedAt = runJob.getStartedAt() == null ? Instant.now() : runJob.getStartedAt();
            Instant endedAt = runJob.getEndedAt() == null ? startedAt : runJob.getEndedAt();
            ps.setString(1, runJob.getStatus().name());
            ps.setInt(2, runJob.getExitCode());
            ps.setString(3, nullToEmpty(runJob.getStdoutText()));
            ps.setString(4, nullToEmpty(runJob.getStderrText()));
            ps.setString(5, nullToEmpty(runJob.getErrorMessage()));
            ps.setString(6, Timestamp.from(startedAt).toString());
            ps.setString(7, Timestamp.from(endedAt).toString());
            ps.setLong(8, runJob.getDurationMs());
            ps.setLong(9, runId);
            ps.setString(10, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update flow run job: " + runId + "/" + jobId, e);
        }
    }

    @Override
    public void deleteOneById(long runId, String jobId) {
        String sql = "DELETE FROM flow_run_job WHERE run_id = ? AND job_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            ps.setString(2, jobId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete flow run job: " + runId + "/" + jobId, e);
        }
    }

    @Override
    public void deleteManyByRunId(long runId) {
        String sql = "DELETE FROM flow_run_job WHERE run_id = ?";
        try (Connection conn = databaseFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, runId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete flow run jobs for run: " + runId, e);
        }
    }

    private static FlowRunJob mapRunJob(ResultSet rs) throws SQLException {
        FlowRunJob record = new FlowRunJob();
        record.setRunId(rs.getLong("run_id"));
        record.setJobId(rs.getString("job_id"));
        record.setStatus(JobStatus.valueOf(rs.getString("status")));
        record.setExitCode(rs.getInt("exit_code"));
        record.setStdoutText(rs.getString("stdout_text"));
        record.setStderrText(rs.getString("stderr_text"));
        record.setErrorMessage(rs.getString("error_message"));
        String startedAt = rs.getString("started_at");
        String endedAt = rs.getString("ended_at");
        if (startedAt != null) {
            record.setStartedAt(Timestamp.valueOf(startedAt).toInstant());
        }
        if (endedAt != null) {
            record.setEndedAt(Timestamp.valueOf(endedAt).toInstant());
        }
        record.setDurationMs(rs.getLong("duration_ms"));
        return record;
    }

    private static void writeRunJob(PreparedStatement ps, FlowRunJob runJob) throws SQLException {
        Instant startedAt = runJob.getStartedAt() == null ? Instant.now() : runJob.getStartedAt();
        Instant endedAt = runJob.getEndedAt() == null ? startedAt : runJob.getEndedAt();
        ps.setLong(1, runJob.getRunId());
        ps.setString(2, nullToEmpty(runJob.getJobId()));
        ps.setString(3, runJob.getStatus().name());
        ps.setInt(4, runJob.getExitCode());
        ps.setString(5, nullToEmpty(runJob.getStdoutText()));
        ps.setString(6, nullToEmpty(runJob.getStderrText()));
        ps.setString(7, nullToEmpty(runJob.getErrorMessage()));
        ps.setString(8, Timestamp.from(startedAt).toString());
        ps.setString(9, Timestamp.from(endedAt).toString());
        ps.setLong(10, runJob.getDurationMs());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

