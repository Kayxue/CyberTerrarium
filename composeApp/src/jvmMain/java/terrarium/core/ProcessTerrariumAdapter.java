package terrarium.core;

import process.ProcessManager;
import process.ProcessTreeNode;
import terrarium.model.TerrariumCreatureKind;
import terrarium.model.TerrariumCreatureSignal;
import terrarium.model.TerrariumCreatureStatus;
import terrarium.model.TerrariumMotionStyle;
import terrarium.model.TerrariumSourceSnapshot;
import terrarium.model.TerrariumSourceStatus;
import terrarium.model.TerrariumVisualHint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ProcessTerrariumAdapter implements TerrariumResourceAdapter {
    public static final String SOURCE_ID = "processes";

    private static final double MEBIBYTE = 1024.0d * 1024.0d;

    private final ProcessManager processManager;

    public ProcessTerrariumAdapter() {
        this(new ProcessManager());
    }

    public ProcessTerrariumAdapter(ProcessManager processManager) {
        if (processManager == null) {
            throw new IllegalArgumentException("Process manager must not be null.");
        }
        this.processManager = processManager;
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Processes";
    }

    @Override
    public TerrariumSourceSnapshot readSnapshot() {
        List<ProcessTreeNode> roots = processManager.getProcessTrees();
        List<TerrariumCreatureSignal> fish = new ArrayList<>();
        Set<Long> emittedPids = new HashSet<>();

        for (ProcessTreeNode root : roots) {
            appendProcessTree(root, fish, emittedPids);
        }

        return new TerrariumSourceSnapshot(
            SOURCE_ID,
            getDisplayName(),
            TerrariumSourceStatus.AVAILABLE,
            List.of(),
            fish,
            Instant.now(),
            ""
        );
    }

    private static void appendProcessTree(
        ProcessTreeNode process,
        List<TerrariumCreatureSignal> fish,
        Set<Long> emittedPids
    ) {
        if (process == null || !emittedPids.add(process.getPid())) {
            return;
        }

        fish.add(toCreatureSignal(process));
        for (ProcessTreeNode child : process.getChildren()) {
            appendProcessTree(child, fish, emittedPids);
        }
    }

    private static TerrariumCreatureSignal toCreatureSignal(ProcessTreeNode process) {
        long pid = process.getPid();
        String pidText = Long.toString(pid);
        String name = normalizedName(process);
        String state = normalizedState(process);
        double cpu = TerrariumMath.clampDouble(process.getCpuUsagePercent(), 0.0d, 100.0d);

        int health = healthFor(state, cpu);
        int stress = stressFor(state, cpu);
        int activity = activityFor(state, cpu);
        int risk = riskFor(state, cpu);
        TerrariumCreatureStatus status = statusFor(state, stress);
        TerrariumMotionStyle motion = motionFor(state, activity, status);

        int colorSeed = (name + "|" + safeText(process.getUser())).hashCode();
        double sizeWeight = sizeWeightFor(process.getResidentMemoryBytes());
        TerrariumVisualHint visualHint = new TerrariumVisualHint(
            colorSeed,
            sizeWeight,
            pid,
            motion,
            TerrariumMath.clampInt(Math.max(activity, risk), 0, 100)
        );

        return new TerrariumCreatureSignal(
            "process:" + pidText,
            name,
            TerrariumCreatureKind.PROCESS,
            SOURCE_ID,
            pidText,
            health,
            stress,
            activity,
            risk,
            status,
            visualHint
        );
    }

    private static int healthFor(String state, double cpu) {
        int baseHealth = switch (state) {
            case "RUNNING" -> 88;
            case "SLEEPING" -> 92;
            case "WAITING" -> 86;
            case "NEW" -> 90;
            case "STOPPED" -> 62;
            case "ZOMBIE" -> 18;
            case "INVALID" -> 12;
            default -> 76;
        };
        int highCpuPenalty = cpu <= 75.0d
            ? 0
            : (int) Math.round((cpu - 75.0d) * 0.25d);
        return TerrariumMath.clampInt(baseHealth - highCpuPenalty, 0, 100);
    }

    private static int stressFor(String state, double cpu) {
        int baseStress = switch (state) {
            case "RUNNING" -> 15;
            case "SLEEPING" -> 5;
            case "WAITING" -> 25;
            case "NEW" -> 20;
            case "STOPPED" -> 30;
            case "ZOMBIE" -> 90;
            case "INVALID" -> 95;
            default -> 35;
        };
        int cpuStress = (int) Math.round(cpu * 0.55d);
        return TerrariumMath.clampInt(baseStress + cpuStress, 0, 100);
    }

    private static int activityFor(String state, double cpu) {
        if ("STOPPED".equals(state) || "INVALID".equals(state)) {
            return 0;
        }
        if ("ZOMBIE".equals(state)) {
            return 5;
        }

        int baseActivity = switch (state) {
            case "RUNNING" -> 25;
            case "WAITING" -> 15;
            case "NEW" -> 20;
            case "SLEEPING" -> 5;
            default -> 10;
        };
        return TerrariumMath.clampInt(baseActivity + (int) Math.round(cpu * 0.75d), 0, 100);
    }

    private static int riskFor(String state, double cpu) {
        int baseRisk = switch (state) {
            case "RUNNING", "SLEEPING", "NEW" -> 8;
            case "WAITING" -> 18;
            case "STOPPED" -> 35;
            case "ZOMBIE" -> 92;
            case "INVALID" -> 96;
            default -> 30;
        };
        int highCpuRisk = cpu <= 85.0d
            ? 0
            : (int) Math.round((cpu - 85.0d) * 0.4d);
        return TerrariumMath.clampInt(baseRisk + highCpuRisk, 0, 100);
    }

    private static TerrariumCreatureStatus statusFor(String state, int stress) {
        return switch (state) {
            case "ZOMBIE", "INVALID" -> TerrariumCreatureStatus.SICK;
            case "STOPPED" -> TerrariumCreatureStatus.INACTIVE;
            default -> stress >= 65
                ? TerrariumCreatureStatus.STRESSED
                : TerrariumCreatureStatus.HEALTHY;
        };
    }

    private static TerrariumMotionStyle motionFor(
        String state,
        int activity,
        TerrariumCreatureStatus status
    ) {
        if (status == TerrariumCreatureStatus.SICK) {
            return TerrariumMotionStyle.DRIFTING;
        }
        if (status == TerrariumCreatureStatus.INACTIVE) {
            return TerrariumMotionStyle.STILL;
        }
        if (activity >= 75) {
            return TerrariumMotionStyle.ERRATIC;
        }
        if (activity >= 35) {
            return TerrariumMotionStyle.ACTIVE;
        }
        if ("SLEEPING".equals(state) || "WAITING".equals(state)) {
            return TerrariumMotionStyle.DRIFTING;
        }
        return TerrariumMotionStyle.CALM;
    }

    private static double sizeWeightFor(long residentMemoryBytes) {
        double memoryMebibytes = Math.max(0L, residentMemoryBytes) / MEBIBYTE;
        return TerrariumMath.clampDouble(
            0.65d + Math.log1p(memoryMebibytes) / 6.0d,
            0.6d,
            1.8d
        );
    }

    private static String normalizedName(ProcessTreeNode process) {
        String name = safeText(process.getName()).trim();
        return name.isEmpty() ? "Process " + process.getPid() : name;
    }

    private static String normalizedState(ProcessTreeNode process) {
        String state = safeText(process.getState()).trim();
        return state.isEmpty() ? "UNKNOWN" : state.toUpperCase(Locale.ROOT);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
