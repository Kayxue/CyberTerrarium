import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SystemUsageHistory {
    private static final SystemUsageHistory EMPTY = new SystemUsageHistory(
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of()
    );

    private final SystemUsageInfo latest;
    private final List<Double> cpu;
    private final List<Double> memory;
    private final List<Double> downloadBytesPerSecond;
    private final List<Double> uploadBytesPerSecond;

    private SystemUsageHistory(
            SystemUsageInfo latest,
            List<Double> cpu,
            List<Double> memory,
            List<Double> downloadBytesPerSecond,
            List<Double> uploadBytesPerSecond
    ) {
        this.latest = latest;
        this.cpu = cpu;
        this.memory = memory;
        this.downloadBytesPerSecond = downloadBytesPerSecond;
        this.uploadBytesPerSecond = uploadBytesPerSecond;
    }

    public static SystemUsageHistory empty() {
        return EMPTY;
    }

    public SystemUsageInfo getLatest() {
        return latest;
    }

    public List<Double> getCpu() {
        return cpu;
    }

    public List<Double> getMemory() {
        return memory;
    }

    public List<Double> getDownloadBytesPerSecond() {
        return downloadBytesPerSecond;
    }

    public List<Double> getUploadBytesPerSecond() {
        return uploadBytesPerSecond;
    }

    public SystemUsageHistory withSample(SystemUsageInfo usage, int maxPoints) {
        return new SystemUsageHistory(
                usage,
                appendBounded(cpu, usage.cpuUsagePercent(), maxPoints),
                appendBounded(memory, usage.memoryUsagePercent(), maxPoints),
                appendBounded(downloadBytesPerSecond, usage.downloadBytesPerSecond(), maxPoints),
                appendBounded(uploadBytesPerSecond, usage.uploadBytesPerSecond(), maxPoints)
        );
    }

    private static List<Double> appendBounded(List<Double> values, double sample, int maxPoints) {
        if (maxPoints <= 0) {
            return List.of();
        }

        int startIndex = Math.max(0, values.size() - maxPoints + 1);
        List<Double> next = new ArrayList<>(Math.min(maxPoints, values.size() + 1));
        for (int i = startIndex; i < values.size(); i++) {
            next.add(values.get(i));
        }
        next.add(sample);
        return Collections.unmodifiableList(next);
    }
}
