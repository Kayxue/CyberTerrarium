package terrarium.model;

public final class TerrariumWaterTint {
    private final int topColorRgb;
    private final int bottomColorRgb;
    private final int hazeColorRgb;

    public TerrariumWaterTint(int topColorRgb, int bottomColorRgb, int hazeColorRgb) {
        this.topColorRgb = normalizeRgb(topColorRgb);
        this.bottomColorRgb = normalizeRgb(bottomColorRgb);
        this.hazeColorRgb = normalizeRgb(hazeColorRgb);
    }

    public static TerrariumWaterTint healthy() {
        return new TerrariumWaterTint(0x81D4FA, 0x0288D1, 0xE0F7FA);
    }

    public static TerrariumWaterTint stressed() {
        return new TerrariumWaterTint(0x8AA8A5, 0x2E5F73, 0xB6B27A);
    }

    public static TerrariumWaterTint sick() {
        return new TerrariumWaterTint(0x8E9A68, 0x3F5F54, 0xC0B65A);
    }

    public int getTopColorRgb() { return topColorRgb; }
    public int getBottomColorRgb() { return bottomColorRgb; }
    public int getHazeColorRgb() { return hazeColorRgb; }

    private static int normalizeRgb(int color) {
        return color & 0xFFFFFF;
    }
}
