package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumEnvironmentSignal {
    private final String id;
    private final String label;
    private final String reason;
    private final int healthImpact;
    private final int stressImpact;
    private final int clarityImpact;
    private final int temperatureImpact;
    private final int motionImpact;
    private final int toxicityImpact;

    public TerrariumEnvironmentSignal(
        String id,
        String label,
        String reason,
        int healthImpact,
        int stressImpact,
        int clarityImpact,
        int temperatureImpact,
        int motionImpact,
        int toxicityImpact
    ) {
        this.id = id == null ? "" : id;
        this.label = label == null ? "" : label;
        this.reason = reason == null ? "" : reason;
        this.healthImpact = TerrariumMath.clampInt(healthImpact, -100, 100);
        this.stressImpact = TerrariumMath.clampInt(stressImpact, 0, 100);
        this.clarityImpact = TerrariumMath.clampInt(clarityImpact, -100, 100);
        this.temperatureImpact = TerrariumMath.clampInt(temperatureImpact, -100, 100);
        this.motionImpact = TerrariumMath.clampInt(motionImpact, -100, 100);
        this.toxicityImpact = TerrariumMath.clampInt(toxicityImpact, -100, 100);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getReason() { return reason; }
    public int getHealthImpact() { return healthImpact; }
    public int getStressImpact() { return stressImpact; }
    public int getClarityImpact() { return clarityImpact; }
    public int getTemperatureImpact() { return temperatureImpact; }
    public int getMotionImpact() { return motionImpact; }
    public int getToxicityImpact() { return toxicityImpact; }
}
