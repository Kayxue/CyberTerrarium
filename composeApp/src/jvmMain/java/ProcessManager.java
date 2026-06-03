public class ProcessManager {

    private final SystemUsageMonitor monitor;
    private final java.util.concurrent.ScheduledExecutorService scheduler;
    private final java.util.List<java.util.function.Consumer<SystemUsageInfo>> listeners;
    private final long intervalMillis;
    private java.util.concurrent.ScheduledFuture<?> task;
    private volatile SystemUsageInfo latest;

    public ProcessManager(long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.monitor = new SystemUsageMonitor();
        this.listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "system-usage-monitor");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() {
        if (task != null && !task.isCancelled()) {
            return;
        }
        task = scheduler.scheduleAtFixedRate(this::sample, 0, intervalMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void addListener(java.util.function.Consumer<SystemUsageInfo> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(java.util.function.Consumer<SystemUsageInfo> listener) {
        listeners.remove(listener);
    }

    public SystemUsageInfo getLatest() {
        return latest;
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    public synchronized void close() {
        stop();
        scheduler.shutdownNow();
    }

    private void sample() {
        SystemUsageInfo usage = monitor.getUsage();
        latest = usage;
        for (java.util.function.Consumer<SystemUsageInfo> listener : listeners) {
            listener.accept(usage);
        }
    }
}
