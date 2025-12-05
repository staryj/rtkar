# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep WebView JavaScript interface
-keepclassmembers class com.ngerasimov.rtkar.ui.login.LoginFragment$WebAppInterface {
    public *;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ngerasimov.rtkar.data.**$$serializer { *; }
-keepclassmembers class com.ngerasimov.rtkar.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.ngerasimov.rtkar.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit (kept for potential future use)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Preserve generic type information for API response wrapper
-keep class com.ngerasimov.rtkar.data.api.RtkApiResponse { *; }
-keep class com.ngerasimov.rtkar.data.api.RtkApiResponse$* { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep data classes for serialization
-keep class com.ngerasimov.rtkar.data.** { *; }
-keep class com.ngerasimov.rtkar.data.api.** { *; }
# Preserve generic signatures for API response types
-keepattributes Signature
-keep class com.ngerasimov.rtkar.data.api.RtkApiResponse { *; }
-keep class com.ngerasimov.rtkar.data.api.DevicesApiResponse { *; }
-keep class com.ngerasimov.rtkar.data.api.DevicesData { *; }
-keep class com.ngerasimov.rtkar.data.api.Device { *; }
-keep class com.ngerasimov.rtkar.data.api.Capability { *; }
-keep class com.ngerasimov.rtkar.data.api.InterCode { *; }

# Keep API error classes
-keep class com.ngerasimov.rtkar.data.api.ApiError { *; }
-keep class com.ngerasimov.rtkar.data.api.ApiError$* { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# Keep Application class
-keep class com.ngerasimov.rtkar.RTKarApplication { *; }
-keep class com.ngerasimov.rtkar.RTKarApplication$* { *; }

# Keep Hilt generated classes
-keep class com.ngerasimov.rtkar.Hilt_* { *; }
-keep class com.ngerasimov.rtkar.*_HiltModules { *; }
-keep class com.ngerasimov.rtkar.*_HiltModules$* { *; }
-keep class com.ngerasimov.rtkar.*Module { *; }
-keep class com.ngerasimov.rtkar.*Module$* { *; }
-keep class com.ngerasimov.rtkar.di.* { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.codegen.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.android.internal.builders.** { *; }

# Keep all Hilt generated injectors and factories
-keep class com.ngerasimov.rtkar.*_GeneratedInjector { *; }
-keep class com.ngerasimov.rtkar.*_MembersInjector { *; }
-keep class com.ngerasimov.rtkar.*_Factory { *; }
-keep class com.ngerasimov.rtkar.*_HiltModules { *; }

-dontwarn com.ngerasimov.rtkar.Hilt_RTKarApplication
-dontwarn dagger.hilt.android.internal.**