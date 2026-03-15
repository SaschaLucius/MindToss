# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class lukulent.mindtoss.app.**$$serializer { *; }
-keepclassmembers class lukulent.mindtoss.app.** {
    *** Companion;
}
-keepclasseswithmembers class lukulent.mindtoss.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
