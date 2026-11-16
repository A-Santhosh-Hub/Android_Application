# ============================================================
# SanDen ProGuard / R8 Rules
# ============================================================

# ---- General ------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ---- Kotlin -------------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---- Coroutines ---------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---- Jetpack Compose ----------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---- Room ---------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ---- Hilt / Dagger ------------------------------------------
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.* <methods>;
    @javax.inject.* <methods>;
    @dagger.* <fields>;
    @javax.inject.* <fields>;
}
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.Module class *
-keep @dagger.hilt.EntryPoint class *

# ---- Coil ---------------------------------------------------
-dontwarn io.coil.**
-keep class io.coil.** { *; }

# ---- Media3 / ExoPlayer ------------------------------------
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# ---- DataStore ----------------------------------------------
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ---- Enums --------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Parcelable ---------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---- Serializable -------------------------------------------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- R8 full mode adjustments -------------------------------
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Keep application entry points --------------------------
-keep class com.sanden.** { *; }
