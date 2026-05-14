package db;

public final class Migration {
    private final int version;
    private final String description;
    private final String sql;
    public Migration(int version, String description, String sql) {
        this.version = version;
        this.description = description;
        this.sql = sql;
    }
    public int getVersion() {
        return version;
    }
    public String getDescription() {
        return description;
    }
    public String getSql() {
        return sql;
    }
}