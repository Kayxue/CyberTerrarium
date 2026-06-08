package notification.service;

import notification.model.Notification;

import java.awt.*;
import java.net.URL;
import java.util.Objects;

public class SystemNotification implements AutoCloseable {
    private static SystemNotification instance;

    public static synchronized SystemNotification getInstance() {
        if (instance == null) {
            instance = new SystemNotification();
        }
        return instance;
    }

    private static final String DEFAULT_TRAY_ICON_RESOURCE = "notification/tray.png";
    private final Object lock = new Object();
    private TrayIcon trayIcon;
    private boolean initialized = false;
    private boolean supported = false;

    public SystemNotification() {
    }

    public boolean isSupported() {
        ensureInitialized();
        return supported;
    }

    public void notify(String title, String info, Notification.Status status) {
        Notification safeModel = new Notification(title, info, status);
        ensureInitialized();
        saveNotificationToDb(safeText(safeModel.getTitle()), safeText(safeModel.getInfo()), safeModel.getStatus());
        if (!supported || trayIcon == null) {
            return;
        }
        String safeTitle = safeText(safeModel.getTitle());
        String content = safeText(safeModel.getInfo());
        TrayIcon.MessageType type = toMessageType(safeModel.getStatus());
        trayIcon.displayMessage(safeTitle, content, type);
    }

    private void saveNotificationToDb(String title, String message, Notification.Status status) {
        try (java.sql.Connection conn = db.DatabaseFactory.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO notification_log (title, message, status) VALUES (?, ?, ?)")) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, status.name());
            ps.executeUpdate();
        } catch (java.sql.SQLException ignored) {
        }
    }

    public static class LogEntry {
        private final int id;
        private final String title;
        private final String message;
        private final String status;
        private final String createdAt;

        public LogEntry(int id, String title, String message, String status, String createdAt) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.status = status;
            this.createdAt = createdAt;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public String getStatus() {
            return status;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static java.util.List<LogEntry> getNotificationLogs() {
        java.util.List<LogEntry> list = new java.util.ArrayList<>();
        try (java.sql.Connection conn = db.DatabaseFactory.getInstance().getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT id, title, message, status, created_at FROM notification_log ORDER BY id DESC")) {
            while (rs.next()) {
                list.add(new LogEntry(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getString("created_at")
                ));
            }
        } catch (java.sql.SQLException ignored) {
        }
        return list;
    }

    public static void clearNotificationLogs() {
        try (java.sql.Connection conn = db.DatabaseFactory.getInstance().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM notification_log");
        } catch (java.sql.SQLException ignored) {
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (trayIcon != null && supported) {
                try {
                    SystemTray.getSystemTray().remove(trayIcon);
                } catch (Exception ignored) {
                }
            }
            trayIcon = null;
            initialized = false;
            supported = false;
        }
    }

    private void ensureInitialized() {
        synchronized (lock) {
            if (initialized) {
                return;
            }
            initialized = true;
            supported = false;
            try {
                if (!SystemTray.isSupported()) {
                    return;
                }
                URL url = Thread.currentThread().getContextClassLoader().getResource(SystemNotification.DEFAULT_TRAY_ICON_RESOURCE);
                Image trayImage = java.awt.Toolkit.getDefaultToolkit().getImage(url);
                String appName = "CyberTerrarium";
                TrayIcon icon = new TrayIcon(trayImage, appName);
                icon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(icon);
                trayIcon = icon;
                supported = true;
            } catch (HeadlessException | AWTException | SecurityException e) {
                trayIcon = null;
                supported = false;
            }
        }
    }

    private TrayIcon.MessageType toMessageType(Notification.Status status) {
        Notification.Status safe = Objects.requireNonNullElse(status, Notification.Status.INFO);
        return switch (safe) {
            case ERROR -> TrayIcon.MessageType.ERROR;
            case WARNING -> TrayIcon.MessageType.WARNING;
            case INFO -> TrayIcon.MessageType.INFO;
            case NONE -> TrayIcon.MessageType.NONE;
        };
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
