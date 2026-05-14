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
            // Add migrations here
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