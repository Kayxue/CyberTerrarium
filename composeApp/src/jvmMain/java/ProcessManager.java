import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

public class ProcessManager {

    private final SystemUsageMonitor monitor;
    private final ScheduledExecutorService scheduler;
    private final List<Consumer<SystemUsageInfo>> listeners;
    private final long intervalMillis;
    private ScheduledFuture<?> task;
    private volatile SystemUsageInfo latest;

    public ProcessManager(long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.monitor = new SystemUsageMonitor();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
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

    public void addListener(Consumer<SystemUsageInfo> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<SystemUsageInfo> listener) {
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
        for (Consumer<SystemUsageInfo> listener : listeners) {
            listener.accept(usage);
        }
    }
}
