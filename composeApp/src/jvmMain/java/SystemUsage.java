import oshi.SystemInfo;
import oshi.util.Util;

public class SystemUsage {
    public double cpuLoad;

    public SystemUsage(){
        var sysInfo = new SystemInfo();
        var hardware = sysInfo.getHardware();
        var processor = hardware.getProcessor();
        var prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(1000);

        cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);
    }

    public static SystemUsage getSystemUsage(){
        return new SystemUsage();
    }
}
