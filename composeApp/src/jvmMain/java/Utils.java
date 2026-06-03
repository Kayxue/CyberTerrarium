import java.util.Locale;

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
        long safeBytesPerSecond = Math.max(0, bytesPerSecond);
        double value = safeBytesPerSecond;
        String[] units = {"B/s", "KB/s", "MB/s", "GB/s", "TB/s"};
        int unitIndex = 0;

        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return safeBytesPerSecond + " " + units[unitIndex];
        }
        if (value >= 100.0) {
            return String.format(Locale.ROOT, "%.0f %s", value, units[unitIndex]);
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }
}
