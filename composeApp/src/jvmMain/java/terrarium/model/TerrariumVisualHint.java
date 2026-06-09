package terrarium.model;

import terrarium.core.TerrariumMath;

public final class TerrariumVisualHint {
    private final int colorSeed;
    private final double sizeWeight;
    private final long positionSeed;
    private final TerrariumMotionStyle motionStyle;
    private final int importance;

    public TerrariumVisualHint(
        int colorSeed,
        double sizeWeight,
        long positionSeed,
        TerrariumMotionStyle motionStyle
    ) {
        this(colorSeed, sizeWeight, positionSeed, motionStyle, 50);
    }

    public TerrariumVisualHint(
        int colorSeed,
        double sizeWeight,
        long positionSeed,
        TerrariumMotionStyle motionStyle,
        int importance
    ) {
        this.colorSeed = colorSeed;
        this.sizeWeight = TerrariumMath.clampDouble(sizeWeight, 0.5d, 2.0d);
        this.positionSeed = positionSeed;
        this.motionStyle = motionStyle == null ? TerrariumMotionStyle.CALM : motionStyle;
        this.importance = TerrariumMath.clampInt(importance, 0, 100);
    }

    public static TerrariumVisualHint stable(String id, double sizeWeight, TerrariumMotionStyle motionStyle) {
        return stable(id, sizeWeight, motionStyle, 50);
    }

    public static TerrariumVisualHint stable(
        String id,
        double sizeWeight,
        TerrariumMotionStyle motionStyle,
        int importance
    ) {
        int hash = id == null ? 0 : id.hashCode();
        return new TerrariumVisualHint(hash, sizeWeight, hash, motionStyle, importance);
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

    public int getImportance() {
        return importance;
    }
}
