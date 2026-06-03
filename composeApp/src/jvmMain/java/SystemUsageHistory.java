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
    private final List<Double> downloadKb;
    private final List<Double> uploadKb;

    private SystemUsageHistory(
            SystemUsageInfo latest,
            List<Double> cpu,
            List<Double> memory,
            List<Double> downloadKb,
            List<Double> uploadKb
    ) {
        this.latest = latest;
        this.cpu = cpu;
        this.memory = memory;
        this.downloadKb = downloadKb;
        this.uploadKb = uploadKb;
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

    public List<Double> getDownloadKb() {
        return downloadKb;
    }

    public List<Double> getUploadKb() {
        return uploadKb;
    }

    public SystemUsageHistory withSample(SystemUsageInfo usage, int maxPoints) {
        return new SystemUsageHistory(
                usage,
                appendBounded(cpu, usage.cpuUsagePercent(), maxPoints),
                appendBounded(memory, usage.memoryUsagePercent(), maxPoints),
                appendBounded(downloadKb, usage.downloadBytesPerSecond() / 1024.0, maxPoints),
                appendBounded(uploadKb, usage.uploadBytesPerSecond() / 1024.0, maxPoints)
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
