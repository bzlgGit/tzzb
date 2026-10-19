# Keep the public integration interface stable when the consuming app enables R8.
-keep public class com.hexin.screenlog.api.** { public *; }

# This Activity is instantiated from the merged library manifest.
-keep class com.hexin.screenlog.ui.ScreenLogSettingsActivity { *; }
