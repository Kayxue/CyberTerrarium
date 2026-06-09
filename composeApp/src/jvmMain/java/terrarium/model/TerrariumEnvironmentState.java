package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumEnvironmentState {
    private final int health;
    private final int stress;
    private final int clarity;
    private final int temperatureStress;
    private final int motion;
    private final int toxicity;
    private final double waveIntensity;
    private final double bubbleIntensity;
    private final TerrariumWaterTint tint;

    public TerrariumEnvironmentState(
        int health,
        int stress,
        int clarity,
        int temperatureStress,
        int motion,
        int toxicity,
        double waveIntensity,
        double bubbleIntensity,
        TerrariumWaterTint tint
    ) {
        this.health = TerrariumMath.clampInt(health, 0, 100);
        this.stress = TerrariumMath.clampInt(stress, 0, 100);
        this.clarity = TerrariumMath.clampInt(clarity, 0, 100);
        this.temperatureStress = TerrariumMath.clampInt(temperatureStress, 0, 100);
        this.motion = TerrariumMath.clampInt(motion, 0, 100);
        this.toxicity = TerrariumMath.clampInt(toxicity, 0, 100);
        this.waveIntensity = TerrariumMath.clampDouble(waveIntensity, 0.0d, 1.0d);
        this.bubbleIntensity = TerrariumMath.clampDouble(bubbleIntensity, 0.0d, 1.0d);
        this.tint = tint == null ? TerrariumWaterTint.healthy() : tint;
    }

    public static TerrariumEnvironmentState healthy() {
        return new TerrariumEnvironmentState(100, 0, 100, 0, 15, 0, 0.2d, 0.35d, TerrariumWaterTint.healthy());
    }

    public int getHealth() { return health; }
    public int getStress() { return stress; }
    public int getClarity() { return clarity; }
    public int getTemperatureStress() { return temperatureStress; }
    public int getMotion() { return motion; }
    public int getToxicity() { return toxicity; }
    public double getWaveIntensity() { return waveIntensity; }
    public double getBubbleIntensity() { return bubbleIntensity; }
    public TerrariumWaterTint getTint() { return tint; }
}
