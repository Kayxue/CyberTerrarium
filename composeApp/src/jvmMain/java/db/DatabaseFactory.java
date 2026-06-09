package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public final class DatabaseFactory {
    private static final String JDBC_URL = getJdbcUrl();
    private static final DatabaseFactory INSTANCE = new DatabaseFactory();
    private DatabaseFactory() {}
    private static String getJdbcUrl() {
        String userHome = System.getProperty("user.home");
        java.io.File dir = new java.io.File(userHome, ".cyberterrarium");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        java.io.File dbFile = new java.io.File(dir, "cyberterrarium.db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }
    public static DatabaseFactory getInstance() {
        return INSTANCE;
    }
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}