package terrarium.core;

public final class TerrariumMath {
    private TerrariumMath() {}

    public static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int percentage(double value) {
        return clampInt((int) Math.round(value), 0, 100);
    }
}
