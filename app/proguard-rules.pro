# ProGuard / R8 rules for Crucilux (Release Build)

# --------------------------------------------------------------------------------------
# Room Persistence Library (v4)
# --------------------------------------------------------------------------------------
# Preserve Room database abstract class, generated implementations, and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.neuronovaapps.crucilux.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# --------------------------------------------------------------------------------------
# Crucilux Master Word Bank & Domain Models (v1.37)
# --------------------------------------------------------------------------------------
# Preserve domain models used by CruciluxBankRepository and game engine
-keep class com.neuronovaapps.crucilux.model.** { *; }
-keepclassmembers enum com.neuronovaapps.crucilux.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
}
-keep class com.neuronovaapps.crucilux.data.bank.** { *; }

# --------------------------------------------------------------------------------------
# DataStore Preferences & Session State
# --------------------------------------------------------------------------------------
-keep class com.neuronovaapps.crucilux.data.TextSizePreference { *; }
-keep class com.neuronovaapps.crucilux.data.GameSessionState { *; }

# --------------------------------------------------------------------------------------
# Jetpack Compose Runtime
# --------------------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
