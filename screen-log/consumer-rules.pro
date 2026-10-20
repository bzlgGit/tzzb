# Keep the supported integration interface stable when the consuming app enables R8.
-keep class com.hexin.screenlog.api.ScreenLogKit { *; }
-keep class com.hexin.screenlog.api.ScreenLog { *; }
-keep class com.hexin.screenlog.api.ScreenLogLevel { *; }
-keep class com.hexin.screenlog.api.ScreenLogRequestPhase { *; }
-keep class com.hexin.screenlog.api.RequestLogMessageProvider { *; }

# This Activity is instantiated from the merged library manifest.
-keep class com.hexin.screenlog.ui.ScreenLogSettingsActivity { *; }
