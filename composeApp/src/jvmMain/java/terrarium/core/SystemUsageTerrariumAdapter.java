package terrarium.core;

import terrarium.model.TerrariumEnvironmentSignal;
import terrarium.model.TerrariumSourceSnapshot;
import terrarium.model.TerrariumSourceStatus;
import terrarium.model.TerrariumSystemMetrics;

import java.time.Instant;
import java.util.List;

public final class SystemUsageTerrariumAdapter implements TerrariumResourceAdapter {
    public static final String SOURCE_ID = "system-usage";

    private final TerrariumSystemMetrics metrics;

    public SystemUsageTerrariumAdapter(TerrariumSystemMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public String getDisplayName() {
        return "System Usage";
    }

    @Override
    public TerrariumSourceSnapshot readSnapshot() {
        if (metrics == null) {
            return TerrariumSourceSnapshot.unavailable(
                SOURCE_ID,
                getDisplayName(),
                "System usage sample is not available yet."
            );
        }

        double cpu = metrics.getCpuUsagePercent();
        double memory = metrics.getMemoryUsagePercent();
        double temperature = metrics.getCpuTemperature();
        double networkMegabytes = (metrics.getDownloadBytesPerSecond() + metrics.getUploadBytesPerSecond()) / 1_048_576.0d;

        int cpuPenalty = penaltyAbove(cpu, 60.0d, 0.75d, 30);
        int memoryPenalty = penaltyAbove(memory, 70.0d, 0.85d, 28);
        int temperaturePenalty = temperature <= 0.0d ? 0 : penaltyAbove(temperature, 75.0d, 1.45d, 30);
        int networkMotion = TerrariumMath.clampInt((int) Math.round(Math.log1p(networkMegabytes) * 18.0d), 0, 35);
        int totalPenalty = TerrariumMath.clampInt(cpuPenalty + memoryPenalty + temperaturePenalty, 0, 88);
        int stressImpact = TerrariumMath.clampInt(totalPenalty + networkMotion / 2, 0, 100);
        int clarityImpact = -TerrariumMath.clampInt(memoryPenalty + temperaturePenalty / 2 + cpuPenalty / 3, 0, 70);
        int toxicityImpact = TerrariumMath.clampInt(temperaturePenalty + memoryPenalty / 2, 0, 85);

        TerrariumEnvironmentSignal signal = new TerrariumEnvironmentSignal(
            "system-pressure",
            "System pressure",
            "CPU, memory, temperature, and network pressure.",
            -totalPenalty,
            stressImpact,
            clarityImpact,
            temperaturePenalty,
            networkMotion,
            toxicityImpact
        );

        return new TerrariumSourceSnapshot(
            SOURCE_ID,
            getDisplayName(),
            TerrariumSourceStatus.AVAILABLE,
            List.of(signal),
            List.of(),
            Instant.now(),
            ""
        );
    }

    private static int penaltyAbove(double value, double threshold, double multiplier, int maxPenalty) {
        if (value <= threshold) {
            return 0;
        }
        return TerrariumMath.clampInt((int) Math.round((value - threshold) * multiplier), 0, maxPenalty);
    }
}
