# ProGuard Rules for Safiri Production Release

# Preserve Line Numbers for Crash Reporting / Play Console De-obfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Moshi JSON Converter
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase

# Firebase (Auth, Firestore, App Check)
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# Models & Data Classes (Prevent field obfuscation for Firestore serialization)
-keep class com.example.models.** { *; }
-keep class com.example.database.** { *; }
-keepclassmembers class com.example.models.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.example.database.** {
    <fields>;
    <init>(...);
}

# Google Play Services & Maps
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
