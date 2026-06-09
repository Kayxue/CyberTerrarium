# ============================================================
# CyberTerrarium ProGuard Rules
# ============================================================

# ProGuard 7.9.1 + JDK 25 (Temurin, without jmods) cannot resolve
# java.lang.Object because there's no rt.jar and jmods/ isn't bundled.
# All three analysis passes (optimize, shrink preverify) need the full
# class hierarchy, so we disable everything except obfuscation-off (already
# set by Compose) and keep the -dontwarn suppression below.

-dontoptimize
-dontshrink
-dontpreverify

# Suppress warnings for unresolved JDK/JRE classes.
-dontwarn java.**
-dontwarn javax.**
-dontwarn sun.**
-dontwarn com.sun.**

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

# SLF4J / SQLite / Kotlin
-dontwarn org.slf4j.**
-dontwarn org.sqlite.**
-dontwarn kotlin.**

# Suppress remaining warnings
-ignorewarnings
