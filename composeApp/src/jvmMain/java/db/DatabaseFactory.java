package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public final class DatabaseFactory {
    private static final String JDBC_URL = buildJdbcUrl();
    private static final DatabaseFactory INSTANCE = new DatabaseFactory();
    private DatabaseFactory() {}

    private static String buildJdbcUrl() {
        // Use ~/Library/Application Support/CyberTerrarium/ on macOS.
        // Falls back to the user home directory on other platforms.
        String appSupportDir = System.getProperty("user.home") + "/Library/Application Support/CyberTerrarium";
        File dir = new File(appSupportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return "jdbc:sqlite:" + appSupportDir + "/cyberterrarium.db";
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