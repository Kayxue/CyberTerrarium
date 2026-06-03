import java.util.List;

public final class Utils {
    private Utils() {
    }

    public static String formatBytes(long bytes) {
        double kb = 1024.0;
        double mb = kb * 1024.0;
        double gb = mb * 1024.0;

        if (bytes >= gb) {
            return String.format("%.2f GB", bytes / gb);
        }
        if (bytes >= mb) {
            return String.format("%.2f MB", bytes / mb);
        }
        if (bytes >= kb) {
            return String.format("%.2f KB", bytes / kb);
        }
        return bytes + " B";
    }

    public static String formatFrequency(long hz) {
        if (hz <= 0) {
            return "N/A";
        }

        double khz = 1000.0;
        double mhz = khz * 1000.0;
        double ghz = mhz * 1000.0;

        if (hz >= ghz) {
            return String.format("%.2f GHz", hz / ghz);
        }
        if (hz >= mhz) {
            return String.format("%.0f MHz", hz / mhz);
        }
        if (hz >= khz) {
            return String.format("%.0f kHz", hz / khz);
        }
        return hz + " Hz";
    }

    public static String formatSpeed(long bytesPerSecond) {
        double kbPerSecond = bytesPerSecond / 1024.0;
        return String.format("%.1f KB/s", kbPerSecond);
    }

    public static void addSample(List<Double> target, double value, int maxPoints) {
        target.add(value);
        int overflow = target.size() - maxPoints;
        if (overflow > 0) {
            for (int i = 0; i < overflow; i++) {
                target.removeFirst();
            }
        }
    }
}
