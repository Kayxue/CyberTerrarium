# ============================================================
# CyberTerrarium ProGuard Rules
# ============================================================

# Disable optimization, shrinking, and preverification
-dontoptimize
-dontshrink
-dontpreverify

# Suppress warnings for unresolved JDK/JRE and other library classes.
-dontwarn java.**
-dontwarn javax.**
-dontwarn sun.**
-dontwarn com.sun.**
-dontwarn oshi.**
-dontwarn org.sqlite.**
-dontwarn org.slf4j.**
-dontwarn kotlin.**

# OSHI: Windows-specific platform classes absent on macOS/Linux
-dontwarn oshi.hardware.platform.windows.**
-dontwarn oshi.software.os.windows.**
-dontwarn oshi.driver.windows.**
-dontwarn oshi.platform.windows.**
-dontwarn com.profesorfalken.jpowershell.**
-dontwarn com.sun.jna.**
-dontwarn net.java.dev.jna.**

# OSHI: dynamically referenced Windows-only hardware monitor
-dontwarn io.github.pandalxb.jlibrehardwaremonitor.**

# Keep rules to prevent stripping classes required for runtime or database reflection
-keep class oshi.** { *; }
-keep class org.sqlite.** { *; }
-keep class job.model.** { *; }

# Keep all enum class members to prevent "is not an enum class" reflection failures at runtime
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress remaining warnings
-ignorewarnings
