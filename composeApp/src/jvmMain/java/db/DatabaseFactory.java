package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public final class DatabaseFactory {
    private static final String JDBC_URL = "jdbc:sqlite:cyberterrarium.db";
    private static final DatabaseFactory INSTANCE = new DatabaseFactory();
    private DatabaseFactory() {}
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