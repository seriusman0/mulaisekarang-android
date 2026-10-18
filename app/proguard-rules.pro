# kotlinx.serialization is a pure JVM library (not an Android AAR), so it does not ship
# consumer ProGuard rules automatically — these are the rules the project itself recommends.
# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the generated $serializer companions and serializer() factories for our own
# @Serializable DTOs (data/model/Models.kt) — required for kotlinx.serialization to find them
# via reflection at runtime once R8 has renamed/stripped everything else.
-keep,includedescriptorclasses class com.mulaisekarang.app.**$$serializer { *; }
-keepclassmembers class com.mulaisekarang.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.mulaisekarang.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Hilt / Dagger — keep generated component and module classes.
# R8 can strip or rename them if not explicitly kept, causing DI injection
# failures at runtime despite a clean build.
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclasseswithmembernames class * {
    @javax.inject.Inject <fields>;
}
-keepclasseswithmembernames class * {
    @javax.inject.Inject <init>(...);
}

# ---------------------------------------------------------------------------
# Pusher Java Client — uses reflection internally
# ---------------------------------------------------------------------------
-keep class com.pusher.** { *; }
-dontwarn com.pusher.**

# ---------------------------------------------------------------------------
# OkHttp / Okio — suppress known harmless warnings
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ---------------------------------------------------------------------------
# Coil — image loader
# ---------------------------------------------------------------------------
-dontwarn coil.**

# ---------------------------------------------------------------------------
# Keep source file names and line numbers in stack traces (Crashlytics reads
# these via the mapping file uploaded to Play / Firebase automatically by the
# Crashlytics Gradle plugin).
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
