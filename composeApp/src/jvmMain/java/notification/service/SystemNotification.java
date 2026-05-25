package notification.service;

import notification.model.Notification;

import java.awt.*;
import java.net.URL;
import java.util.Objects;

public class SystemNotification implements AutoCloseable {
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
        if (!supported || trayIcon == null) {
            System.out.println("[NOTIFY][" + safeModel.getStatus() + "] "
                    + safeText(safeModel.getTitle()) + " - " + safeText(safeModel.getInfo()));
            return;
        }
        String safeTitle = safeText(safeModel.getTitle());
        String content = safeText(safeModel.getInfo());
        TrayIcon.MessageType type = toMessageType(safeModel.getStatus());
        trayIcon.displayMessage(safeTitle, content, type);
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
