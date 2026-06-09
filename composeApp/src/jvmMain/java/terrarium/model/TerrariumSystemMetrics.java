package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumSystemMetrics {
    private final double cpuUsagePercent;
    private final long cpuCurrentFrequencyHz;
    private final long memoryUsedBytes;
    private final long memoryTotalBytes;
    private final double memoryUsagePercent;
    private final long downloadBytesPerSecond;
    private final long uploadBytesPerSecond;
    private final double cpuTemperature;

    public TerrariumSystemMetrics(
        double cpuUsagePercent,
        long cpuCurrentFrequencyHz,
        long memoryUsedBytes,
        long memoryTotalBytes,
        double memoryUsagePercent,
        long downloadBytesPerSecond,
        long uploadBytesPerSecond,
        double cpuTemperature
    ) {
        this.cpuUsagePercent = sanitizePercent(cpuUsagePercent);
        this.cpuCurrentFrequencyHz = Math.max(0L, cpuCurrentFrequencyHz);
        this.memoryUsedBytes = Math.max(0L, memoryUsedBytes);
        this.memoryTotalBytes = Math.max(0L, memoryTotalBytes);
        this.memoryUsagePercent = sanitizePercent(memoryUsagePercent);
        this.downloadBytesPerSecond = Math.max(0L, downloadBytesPerSecond);
        this.uploadBytesPerSecond = Math.max(0L, uploadBytesPerSecond);
        this.cpuTemperature = Double.isFinite(cpuTemperature) ? Math.max(0.0d, cpuTemperature) : 0.0d;
    }

    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public long getCpuCurrentFrequencyHz() { return cpuCurrentFrequencyHz; }
    public long getMemoryUsedBytes() { return memoryUsedBytes; }
    public long getMemoryTotalBytes() { return memoryTotalBytes; }
    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public long getDownloadBytesPerSecond() { return downloadBytesPerSecond; }
    public long getUploadBytesPerSecond() { return uploadBytesPerSecond; }
    public double getCpuTemperature() { return cpuTemperature; }

    private static double sanitizePercent(double value) {
        if (!Double.isFinite(value)) {
            return 0.0d;
        }
        return TerrariumMath.clampDouble(value, 0.0d, 100.0d);
    }
}
