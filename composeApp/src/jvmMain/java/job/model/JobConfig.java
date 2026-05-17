package job.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class JobConfig {
    private Duration timeout;
    private int retry;
    private int priority;
    private Map<String, String> attributes;

    public JobConfig() {
        this(Duration.ofSeconds(60), 0, 0, new HashMap<>());
    }

    public JobConfig(Duration timeout, int retry, int priority, Map<String, String> attributes) {
        this.timeout = timeout;
        this.retry = retry;
        this.priority = priority;
        this.attributes = attributes;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
