import oshi.ffm.SystemInfo;
import oshi.hardware.*;

import java.util.List;

public class SystemUsageMonitor {
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final List<NetworkIF> networkIFs;
    private final Sensors sensors;

    private long[] previousCpuTicks;
    private long previousBytesReceived;
    private long previousBytesSent;
    private long previousNetworkSampleTime;

    public SystemUsageMonitor() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.networkIFs = hardware.getNetworkIFs();
        this.sensors = hardware.getSensors();

        this.previousCpuTicks = processor.getSystemCpuLoadTicks();

        updateNetworkInterfaces();
        this.previousBytesReceived = getTotalBytesReceived();
        this.previousBytesSent = getTotalBytesSent();
        this.previousNetworkSampleTime = System.currentTimeMillis();
    }

    public SystemUsageInfo getUsage() {
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100.0;
        previousCpuTicks = processor.getSystemCpuLoadTicks();

        long cpuCurrentFrequency = 0L;
        long[] currentFrequencies = processor.getCurrentFreq();
        if (currentFrequencies != null && currentFrequencies.length > 0) {
            long totalFrequency = 0L;
            for (long frequency : currentFrequencies) {
                totalFrequency += frequency;
            }
            cpuCurrentFrequency = totalFrequency / currentFrequencies.length;
        }

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        double memoryUsage = totalMemory == 0
                ? 0.0
                : usedMemory * 100.0 / totalMemory;

        updateNetworkInterfaces();

        long currentBytesReceived = getTotalBytesReceived();
        long currentBytesSent = getTotalBytesSent();
        long currentTime = System.currentTimeMillis();

        long elapsedMillis = currentTime - previousNetworkSampleTime;

        long downloadBytesPerSecond = elapsedMillis <= 0
                ? 0
                : (currentBytesReceived - previousBytesReceived) * 1000 / elapsedMillis;

        long uploadBytesPerSecond = elapsedMillis <= 0
                ? 0
                : (currentBytesSent - previousBytesSent) * 1000 / elapsedMillis;

        double cpuTemperature = sensors.getCpuTemperature();

        previousBytesReceived = currentBytesReceived;
        previousBytesSent = currentBytesSent;
        previousNetworkSampleTime = currentTime;

        return new SystemUsageInfo(
                cpuUsage,
                cpuCurrentFrequency,
                usedMemory,
                totalMemory,
                memoryUsage,
                currentBytesReceived,
                currentBytesSent,
                downloadBytesPerSecond,
                uploadBytesPerSecond,
                cpuTemperature
        );
    }

    private void updateNetworkInterfaces() {
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
        }
    }

    private long getTotalBytesReceived() {
        long total = 0;

        for (NetworkIF networkIF : networkIFs) {
            total += networkIF.getBytesRecv();
        }

        return total;
    }

    private long getTotalBytesSent() {
        long total = 0;

        for (NetworkIF networkIF : networkIFs) {
            total += networkIF.getBytesSent();
        }

        return total;
    }
}
