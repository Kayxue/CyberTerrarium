import oshi.SystemInfo;

public class SystemUsage {
    public double cpuLoad;

    public SystemUsage(){
        var sysInfo = new SystemInfo();
        var hardware = sysInfo.getHardware();
        var processor = hardware.getProcessor();
        cpuLoad = processor.getSystemCpuLoad(1);
    }

    public static SystemUsage getSystemUsage(){
        return new SystemUsage();
    }
}
