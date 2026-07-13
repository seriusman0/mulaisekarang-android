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
