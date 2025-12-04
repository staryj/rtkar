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

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
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

# Keep API error classes
-keep class com.ngerasimov.rtkar.data.api.ApiError { *; }
-keep class com.ngerasimov.rtkar.data.api.ApiError$* { *; }