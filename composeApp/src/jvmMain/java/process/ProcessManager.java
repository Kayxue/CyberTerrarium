package process;

import oshi.ffm.SystemInfo;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSProcess;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProcessManager {
    private final OperatingSystem operatingSystem;
    private final Map<Integer, OSProcess> previousProcesses = new HashMap<>();

    public ProcessManager() {
        this.operatingSystem = new SystemInfo().getOperatingSystem();
    }

    public synchronized List<ProcessTreeNode> getProcessTrees() {
        List<OSProcess> processes = operatingSystem.getProcesses();
        Map<Integer, OSProcess> currentProcesses = new HashMap<>();
        Map<Integer, ProcessTreeNode> nodesByPid = new HashMap<>();

        for (OSProcess process : processes) {
            int pid = process.getProcessID();
            currentProcesses.put(pid, process);
            nodesByPid.put(pid, ProcessTreeNode.from(process, previousProcesses.get(pid)));
        }

        List<ProcessTreeNode> roots = new ArrayList<>();
        for (ProcessTreeNode node : nodesByPid.values()) {
            ProcessTreeNode parent = nodesByPid.get((int) node.getParentPid());
            if (parent == null || node.getParentPid() <= 0 || parent.getPid() == node.getPid()) {
                roots.add(node);
            } else {
                parent.addChild(node);
            }
        }

        sortTree(roots);
        previousProcesses.clear();
        previousProcesses.putAll(currentProcesses);
        return Collections.unmodifiableList(roots);
    }

    public boolean terminateProcess(long pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return true;
        }

        ProcessHandle processHandle = handle.get();
        if (!processHandle.isAlive()) {
            return true;
        }

        boolean signalSent = processHandle.destroy();
        waitForExit(processHandle, Duration.ofMillis(900));

        if (processHandle.isAlive()) {
            signalSent = processHandle.destroyForcibly() || signalSent;
            waitForExit(processHandle, Duration.ofMillis(900));
        }

        return signalSent && !processHandle.isAlive();
    }

    public TerminationResult terminateProcesses(Collection<Long> pids) {
        int requested = 0;
        int terminated = 0;
        List<Long> failedPids = new ArrayList<>();

        for (Long pid : pids) {
            if (pid == null) {
                continue;
            }

            requested++;
            if (terminateProcess(pid)) {
                terminated++;
            } else {
                failedPids.add(pid);
            }
        }

        return new TerminationResult(requested, terminated, failedPids);
    }

    private void waitForExit(ProcessHandle processHandle, Duration timeout) {
        try {
            processHandle.onExit().get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // ProcessHandle exposes best-effort termination; callers receive final alive state.
        }
    }

    private void sortTree(List<ProcessTreeNode> nodes) {
        nodes.sort(
                Comparator.comparing(ProcessTreeNode::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(ProcessTreeNode::getPid)
        );
        for (ProcessTreeNode node : nodes) {
            sortTree(node.getMutableChildren());
        }
    }
}
