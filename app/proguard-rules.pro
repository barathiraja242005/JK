# Enums are stored by name (Room converters, Firestore fields, saved settings), so their constant names must
# survive shrinking or saved data would no longer read back.
-keepclassmembers enum com.barathiraja.jk.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers in crash reports readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Release builds drop debug/info/warning logs, so nothing about the gym ends up in the phone's log. Errors stay.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
