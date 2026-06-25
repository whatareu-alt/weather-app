# ── Sunnyside Weather — R8 / ProGuard rules ───────────────────────────────────

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

# ── kotlinx.serialization ─────────────────────────────────────────────────────
# (The library ships consumer rules, but keep these explicit for safety.)
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *** descriptor; }

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep every @Serializable type and its members (data models + cached state)
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializer *;
}

# All weather data models are (de)serialized at runtime
-keep class com.example.aiweathermonitor.data.models.** { *; }
-keep class com.example.aiweathermonitor.WeatherState { *; }
-keep class com.example.aiweathermonitor.HourForecast { *; }
-keep class com.example.aiweathermonitor.DayForecast { *; }
-keep class com.example.aiweathermonitor.GeocodingResult { *; }

# ── OkHttp / Okio (bundle their own rules; silence optional warnings) ──────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Lottie ────────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── WorkManager + Room (WorkManager uses a Room DB internally) ────────────────
# R8 was obfuscating Room's generated *_Impl classes, breaking the reflective
# instantiation of WorkDatabase ("Failed to create an instance of ...").
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.**

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Database class * { *; }
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-dontwarn androidx.room.**

# ── DataStore (preferences persistence) ───────────────────────────────────────
-keep class androidx.datastore.*.** { *; }
-dontwarn androidx.datastore.**
