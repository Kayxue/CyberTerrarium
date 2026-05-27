public record SystemUsageInfo(
        double cpuUsagePercent,
        long memoryUsedBytes,
        long memoryTotalBytes,
        double memoryUsagePercent
) {
}
