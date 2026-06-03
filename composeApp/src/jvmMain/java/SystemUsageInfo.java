public record SystemUsageInfo(
        double cpuUsagePercent,
        long cpuCurrentFrequencyHz,
        long memoryUsedBytes,
        long memoryTotalBytes,
        double memoryUsagePercent,
        long bytesReceived,
        long bytesSent,
        long downloadBytesPerSecond,
        long uploadBytesPerSecond,
        double cpuTemperature
) {
}
