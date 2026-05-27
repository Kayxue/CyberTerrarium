import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class SystemUsageMonitor {
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final CentralProcessor processor;
    private final GlobalMemory memory;

    private long[] previousCpuTicks;

    public SystemUsageMonitor() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.previousCpuTicks = processor.getSystemCpuLoadTicks();
    }

    public SystemUsageInfo getUsage() {
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100.0;
        previousCpuTicks = processor.getSystemCpuLoadTicks();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        double memoryUsage = totalMemory == 0
                ? 0.0
                : usedMemory * 100.0 / totalMemory;

        return new SystemUsageInfo(
                cpuUsage,
                usedMemory,
                totalMemory,
                memoryUsage
        );
    }
}
