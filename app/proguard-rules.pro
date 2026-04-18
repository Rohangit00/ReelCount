# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }

# Keep Room entities
-keep class com.reelcount.app.data.db.** { *; }

# Keep enum names for Room type converters
-keepclassmembers enum * { *; }
