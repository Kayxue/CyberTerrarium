package notification.model;

public class Notification {
    public enum Status {
        INFO,
        NONE,
        WARNING,
        ERROR,
        SUCCESS
    }

    private String title = "";
    private String info = "";
    private Status status = Status.INFO;

    public Notification() {
    }

    public Notification(String title, String info, Status status) {
        this.title = title == null ? "" : title;
        this.info = info == null ? "" : info;
        this.status = status == null ? Status.INFO : status;
    }

    public String getTitle() {
        return title;
    }

    public Notification setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public String getInfo() {
        return info;
    }

    public Notification setInfo(String info) {
        this.info = info == null ? "" : info;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Notification setStatus(Status status) {
        this.status = status == null ? Status.INFO : status;
        return this;
    }
}
