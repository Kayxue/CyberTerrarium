package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
public final class MigrationRunner {
    private static final List<Migration> MIGRATIONS = List.of(
        new Migration(1, "create job table", """
            CREATE TABLE IF NOT EXISTS job (
                id TEXT PRIMARY KEY,
                stage_id TEXT NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                script_language TEXT NOT NULL DEFAULT 'JAVA',
                script_content TEXT NOT NULL DEFAULT '',
                enabled INTEGER NOT NULL DEFAULT 1,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """),
        new Migration(2, "create job_config table", """
            CREATE TABLE IF NOT EXISTS job_config (
                job_id TEXT PRIMARY KEY,
                timeout_ms INTEGER NOT NULL DEFAULT 60000,
                retry_count INTEGER NOT NULL DEFAULT 0,
                priority INTEGER NOT NULL DEFAULT 0,
                attributes_text TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(3, "create job_dependency table", """
            CREATE TABLE IF NOT EXISTS job_dependency (
                job_id TEXT NOT NULL,
                upstream_job_id TEXT NOT NULL,
                PRIMARY KEY (job_id, upstream_job_id),
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE,
                FOREIGN KEY (upstream_job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(4, "create flow_trigger table and migrate data", """
            CREATE TABLE IF NOT EXISTS flow_trigger (
                job_id TEXT PRIMARY KEY,
                trigger_type TEXT NOT NULL DEFAULT 'MANUAL',
                interval_ms INTEGER,
                CHECK (trigger_type IN ('MANUAL', 'INTERVAL')),
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(5, "create flow_stage table and migrate data", """
            CREATE TABLE IF NOT EXISTS flow_stage (
                id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL DEFAULT '',
                order_no INTEGER NOT NULL DEFAULT 0,
                barrier_mode TEXT NOT NULL DEFAULT 'SOFT',
                fail_mode TEXT NOT NULL DEFAULT 'STOP'
            )
        """),
        new Migration(6, "create flow_run table and migrate data", """
            CREATE TABLE IF NOT EXISTS flow_run (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flow_id TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'IDLE',
                started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                ended_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """),
        new Migration(7, "create jobs_to_flows table", """
            CREATE TABLE IF NOT EXISTS jobs_to_flows (
                flow_id TEXT NOT NULL,
                job_id TEXT NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (flow_id, job_id),
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(8, "add flow_id to flow_stage", """
            ALTER TABLE flow_stage ADD COLUMN flow_id TEXT NOT NULL DEFAULT '';
            CREATE INDEX IF NOT EXISTS idx_flow_stage_flow_id_order
            ON flow_stage(flow_id, order_no, id)
        """),
        new Migration(9, "drop legacy trigger table", """
            DROP TABLE IF EXISTS job_trigger
        """)
    );
    private MigrationRunner() {}
    public static void migrate() {
        try (Connection conn = DatabaseFactory.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                createMigrationsTable(conn);
                Set<Integer> appliedVersions = loadAppliedVersions(conn);
                for (Migration migration : MIGRATIONS) {
                    if (!appliedVersions.contains(migration.getVersion())) {
                        applyMigration(conn, migration);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Database migration failed", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to open database connection", e);
        }
    }
    private static void createMigrationsTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS schema_migrations (" +
                "version INTEGER PRIMARY KEY, " +
                "description TEXT NOT NULL, " +
                "applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
    private static Set<Integer> loadAppliedVersions(Connection conn) throws SQLException {
        Set<Integer> versions = new LinkedHashSet<>();
        String sql = "SELECT version FROM schema_migrations";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                versions.add(rs.getInt("version"));
            }
        }
        return versions;
    }
    private static void applyMigration(Connection conn, Migration migration) throws SQLException {
        try (Statement st = conn.createStatement()) {
            String[] statements = migration.getSql().split(";");
            for (String raw : statements) {
                String sql = raw.trim();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
        }
        migrateLegacyDataIfNeeded(conn, migration.getVersion());
        String insertSql = "INSERT INTO schema_migrations(version, description) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, migration.getVersion());
            ps.setString(2, migration.getDescription());
            ps.executeUpdate();
        }
    }

    private static void migrateLegacyDataIfNeeded(Connection conn, int version) throws SQLException {
        if (version == 4 && tableExists(conn, "job_trigger")) {
            execute(conn, """
                INSERT OR IGNORE INTO flow_trigger(job_id, trigger_type, interval_ms)
                SELECT
                    job_id,
                    CASE
                        WHEN UPPER(trigger_type) = 'INTERVAL' THEN 'INTERVAL'
                        ELSE 'MANUAL'
                    END,
                    CASE
                        WHEN UPPER(trigger_type) = 'INTERVAL' THEN interval_ms
                        ELSE NULL
                    END
                FROM job_trigger
                """);
        }
        if (version == 5 && tableExists(conn, "job_stage")) {
            execute(conn, """
                INSERT OR IGNORE INTO flow_stage(id, display_name, order_no, barrier_mode, fail_mode)
                SELECT id, display_name, order_no, barrier_mode, fail_mode
                FROM job_stage
                """);
        }
        if (version == 6 && tableExists(conn, "job_result")) {
            execute(conn, """
                INSERT OR IGNORE INTO flow_run(id, flow_id, status, started_at, ended_at)
                SELECT
                    id,
                    job_id,
                    CASE
                        WHEN status IN ('SUCCESS') THEN 'SUCCESS'
                        WHEN status IN ('CANCELLED') THEN 'CANCELLED'
                        WHEN status IN ('FAILED', 'TIMEOUT') THEN 'ERROR'
                        WHEN status IN ('RUNNING', 'QUEUED', 'INITIALIZING') THEN 'PENDING'
                        ELSE 'IDLE'
                    END,
                    ended_at,
                    ended_at
                FROM job_result
                """);
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
