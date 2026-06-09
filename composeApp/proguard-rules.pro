# Disable optimization to prevent bytecode VerifyErrors (Inconsistent stackmap frames in Compose/Kotlin lambdas)
-dontoptimize

# Suppress warnings and keep classes for OSHI core FFM which interacts with native OS APIs
-dontwarn oshi.**
-keep class oshi.** { *; }

# Keep SQLite driver classes to prevent dynamic loading failure at runtime
-dontwarn org.sqlite.**
-keep class org.sqlite.** { *; }

# Ignore unresolved references to JDK 25 FFM / MethodHandle/VarHandle APIs in library classes
-dontwarn java.lang.invoke.**
-dontwarn java.lang.foreign.**

# Keep all job model classes and their fields/methods intact for database mapping and reflection
-keep class job.model.** { *; }

# Keep all enum class members to prevent "is not an enum class" reflection failures at runtime
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
