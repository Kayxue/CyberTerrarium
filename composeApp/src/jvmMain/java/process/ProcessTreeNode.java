package process;

import oshi.software.os.OSProcess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ProcessTreeNode {
    private final long pid;
    private final long parentPid;
    private final String name;
    private final String commandLine;
    private final String user;
    private final String state;
    private final double cpuUsagePercent;
    private final long residentMemoryBytes;
    private final List<ProcessTreeNode> mutableChildren = new ArrayList<>();

    private ProcessTreeNode(
            long pid,
            long parentPid,
            String name,
            String commandLine,
            String user,
            String state,
            double cpuUsagePercent,
            long residentMemoryBytes
    ) {
        this.pid = pid;
        this.parentPid = parentPid;
        this.name = name;
        this.commandLine = commandLine;
        this.user = user;
        this.state = state;
        this.cpuUsagePercent = cpuUsagePercent;
        this.residentMemoryBytes = residentMemoryBytes;
    }

    static ProcessTreeNode from(OSProcess process, OSProcess previousProcess) {
        double cpuLoad = previousProcess == null
                ? process.getProcessCpuLoadCumulative()
                : process.getProcessCpuLoadBetweenTicks(previousProcess);

        return new ProcessTreeNode(
                process.getProcessID(),
                process.getParentProcessID(),
                displayName(process),
                safeText(process.getCommandLine()),
                safeText(process.getUser()),
                process.getState() == null ? "UNKNOWN" : process.getState().name(),
                clampPercent(cpuLoad * 100.0),
                Math.max(0L, process.getResidentMemory())
        );
    }

    private static String displayName(OSProcess process) {
        String name = safeText(process.getName());
        if (!name.isBlank()) {
            return name;
        }

        String path = safeText(process.getPath());
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (slash >= 0 && slash < path.length() - 1) {
            return path.substring(slash + 1);
        }
        return "Process " + process.getProcessID();
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static double clampPercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return Math.min(100.0, value);
    }

    private static String formatBytes(long bytes) {
        double kb = 1024.0;
        double mb = kb * 1024.0;
        double gb = mb * 1024.0;

        if (bytes >= gb) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / gb);
        }
        if (bytes >= mb) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / mb);
        }
        if (bytes >= kb) {
            return String.format(Locale.ROOT, "%.2f KB", bytes / kb);
        }
        return bytes + " B";
    }

    void addChild(ProcessTreeNode child) {
        mutableChildren.add(child);
    }

    List<ProcessTreeNode> getMutableChildren() {
        return mutableChildren;
    }

    public long getPid() {
        return pid;
    }

    public long getParentPid() {
        return parentPid;
    }

    public String getName() {
        return name;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public String getUser() {
        return user;
    }

    public String getState() {
        return state;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public String getCpuUsageText() {
        return String.format(Locale.ROOT, "%.1f%%", cpuUsagePercent);
    }

    public long getResidentMemoryBytes() {
        return residentMemoryBytes;
    }

    public String getMemoryUsageText() {
        return formatBytes(residentMemoryBytes);
    }

    public List<ProcessTreeNode> getChildren() {
        return Collections.unmodifiableList(mutableChildren);
    }
}
