package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumVisualHint {
    private final int colorSeed;
    private final double sizeWeight;
    private final long positionSeed;
    private final TerrariumMotionStyle motionStyle;

    public TerrariumVisualHint(
        int colorSeed,
        double sizeWeight,
        long positionSeed,
        TerrariumMotionStyle motionStyle
    ) {
        this.colorSeed = colorSeed;
        this.sizeWeight = TerrariumMath.clampDouble(sizeWeight, 0.5d, 2.0d);
        this.positionSeed = positionSeed;
        this.motionStyle = motionStyle == null ? TerrariumMotionStyle.CALM : motionStyle;
    }

    public static TerrariumVisualHint stable(String id, double sizeWeight, TerrariumMotionStyle motionStyle) {
        int hash = id == null ? 0 : id.hashCode();
        return new TerrariumVisualHint(hash, sizeWeight, hash, motionStyle);
    }

    public int getColorSeed() {
        return colorSeed;
    }

    public double getSizeWeight() {
        return sizeWeight;
    }

    public long getPositionSeed() {
        return positionSeed;
    }

    public TerrariumMotionStyle getMotionStyle() {
        return motionStyle;
    }
}
