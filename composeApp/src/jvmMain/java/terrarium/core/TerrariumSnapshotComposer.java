package terrarium.core;

import terrarium.model.TerrariumCreatureSignal;
import terrarium.model.TerrariumEnvironmentSignal;
import terrarium.model.TerrariumEnvironmentState;
import terrarium.model.TerrariumFishState;
import terrarium.model.TerrariumSnapshot;
import terrarium.model.TerrariumSourceSnapshot;
import terrarium.model.TerrariumWaterTint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TerrariumSnapshotComposer {
    public TerrariumSnapshot composeSnapshots(List<TerrariumSourceSnapshot> sourceSnapshots) {
        List<TerrariumSourceSnapshot> snapshots = List.copyOf(sourceSnapshots == null ? List.of() : sourceSnapshots);
        TerrariumEnvironmentState environment = composeEnvironment(snapshots);
        List<TerrariumFishState> fish = composeFish(snapshots, environment);
        return new TerrariumSnapshot(environment, fish, snapshots, Instant.now());
    }

    private static TerrariumEnvironmentState composeEnvironment(List<TerrariumSourceSnapshot> snapshots) {
        int healthImpact = 0;
        int stress = 0;
        int clarityImpact = 0;
        int temperatureStress = 0;
        int motion = 15;
        int toxicity = 0;

        for (TerrariumSourceSnapshot snapshot : snapshots) {
            for (TerrariumEnvironmentSignal signal : snapshot.getEnvironmentSignals()) {
                healthImpact += signal.getHealthImpact();
                stress += signal.getStressImpact();
                clarityImpact += signal.getClarityImpact();
                temperatureStress += Math.max(0, signal.getTemperatureImpact());
                motion += signal.getMotionImpact();
                toxicity += Math.max(0, signal.getToxicityImpact());
            }
        }

        int normalizedHealth = TerrariumMath.clampInt(100 + healthImpact, 0, 100);
        int normalizedStress = TerrariumMath.clampInt(stress, 0, 100);
        int normalizedClarity = TerrariumMath.clampInt(100 + clarityImpact - toxicity / 2, 0, 100);
        int normalizedTemperature = TerrariumMath.clampInt(temperatureStress, 0, 100);
        int normalizedMotion = TerrariumMath.clampInt(motion, 0, 100);
        int normalizedToxicity = TerrariumMath.clampInt(toxicity, 0, 100);
        double waveIntensity = TerrariumMath.clampDouble(0.15d + normalizedMotion / 140.0d + normalizedStress / 280.0d, 0.0d, 1.0d);
        double bubbleIntensity = TerrariumMath.clampDouble(0.25d + (100 - normalizedHealth) / 240.0d + normalizedMotion / 300.0d, 0.0d, 1.0d);

        TerrariumWaterTint tint;
        if (normalizedHealth < 45 || normalizedToxicity > 50) {
            tint = TerrariumWaterTint.sick();
        } else if (normalizedHealth < 75 || normalizedStress > 45) {
            tint = TerrariumWaterTint.stressed();
        } else {
            tint = TerrariumWaterTint.healthy();
        }

        return new TerrariumEnvironmentState(
            normalizedHealth,
            normalizedStress,
            normalizedClarity,
            normalizedTemperature,
            normalizedMotion,
            normalizedToxicity,
            waveIntensity,
            bubbleIntensity,
            tint
        );
    }

    private static List<TerrariumFishState> composeFish(
        List<TerrariumSourceSnapshot> snapshots,
        TerrariumEnvironmentState environment
    ) {
        List<TerrariumFishState> fish = new ArrayList<>();
        for (TerrariumSourceSnapshot snapshot : snapshots) {
            for (TerrariumCreatureSignal signal : snapshot.getCreatureSignals()) {
                fish.add(TerrariumFishState.fromSignal(signal, environment));
            }
        }
        fish.sort(Comparator.comparing(TerrariumFishState::getSourceId).thenComparing(TerrariumFishState::getId));
        return List.copyOf(fish);
    }
}
