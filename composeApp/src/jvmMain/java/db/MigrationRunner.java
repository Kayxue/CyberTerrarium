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
        new Migration(3, "create job_trigger table", """
            CREATE TABLE IF NOT EXISTS job_trigger (
                job_id TEXT PRIMARY KEY,
                trigger_type TEXT NOT NULL DEFAULT 'MANUAL',
                interval_ms INTEGER,
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(4, "create job_dependency table", """
            CREATE TABLE IF NOT EXISTS job_dependency (
                job_id TEXT NOT NULL,
                upstream_job_id TEXT NOT NULL,
                PRIMARY KEY (job_id, upstream_job_id),
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE,
                FOREIGN KEY (upstream_job_id) REFERENCES job(id) ON DELETE CASCADE
            )
        """),
        new Migration(5, "create job_stage table", """
            CREATE TABLE IF NOT EXISTS job_stage (
                id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL DEFAULT '',
                order_no INTEGER NOT NULL DEFAULT 0,
                barrier_mode TEXT NOT NULL DEFAULT 'SOFT',
                fail_mode TEXT NOT NULL DEFAULT 'STOP'
            )
        """),
        new Migration(6, "create job_result table", """
            CREATE TABLE IF NOT EXISTS job_result (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                job_id TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'IDLE',
                ended_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE
            )
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
            st.execute(migration.getSql());
        }
        String insertSql = "INSERT INTO schema_migrations(version, description) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, migration.getVersion());
            ps.setString(2, migration.getDescription());
            ps.executeUpdate();
        }
    }
}
