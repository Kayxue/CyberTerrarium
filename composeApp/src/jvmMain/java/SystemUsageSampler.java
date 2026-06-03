public final class SystemUsageSampler {
    private final SystemUsageMonitor monitor;
    private final int maxPoints;
    private SystemUsageHistory history;

    public SystemUsageSampler(int maxPoints) {
        this(new SystemUsageMonitor(), maxPoints);
    }

    public SystemUsageSampler(SystemUsageMonitor monitor, int maxPoints) {
        this.monitor = monitor;
        this.maxPoints = maxPoints;
        this.history = SystemUsageHistory.empty();
    }

    public SystemUsageInfo sampleLatest() {
        return sampleHistory().getLatest();
    }

    public SystemUsageHistory sampleHistory() {
        history = history.withSample(monitor.getUsage(), maxPoints);
        return history;
    }

    public SystemUsageHistory getHistory() {
        return history;
    }
}
