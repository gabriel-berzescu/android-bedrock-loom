# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep AWS SDK classes
-keep class aws.sdk.kotlin.** { *; }
-keep class aws.smithy.kotlin.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain models for serialization
-keep,includedescriptorclasses class com.loom.bedrock.domain.model.**$$serializer { *; }
-keepclassmembers class com.loom.bedrock.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.loom.bedrock.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
