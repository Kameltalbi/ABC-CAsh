-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-dontwarn com.google.api.client.**
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.migration.Migration
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Enums (TypeConverters)
-keepclassmembers enum com.abccash.app.treasury.data.** { *; }

# Domain / local models
-keep class com.abccash.app.treasury.data.** { *; }
-keep class com.abccash.app.treasury.local.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Kotlin coroutines / metadata
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}
-keep class kotlin.Metadata { *; }

# Google Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
