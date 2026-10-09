# Smart Vault release R8 rules
#
# Keep this file intentionally narrow. Broad rules such as
# -keep class com.example.** { *; }
# would defeat obfuscation and recreate the Google Play warning.

# Preserve enough metadata for reliable crash deobfuscation / Retrace while
# still allowing class, method and field names to be obfuscated.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Libraries that inspect generic signatures / annotations at runtime can rely
# on these attributes. Keeping metadata does not disable name obfuscation.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# WorkManager persists worker class names in its local database. Preserve our
# worker class names so pending scheduled work survives app upgrades where the
# R8 mapping can change between releases.
-keepnames class com.example.worker.** extends androidx.work.ListenableWorker
-keepclassmembers class com.example.worker.** extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room, Firebase, Retrofit, Moshi, Compose and AndroidX WorkManager ship their
# own consumer ProGuard/R8 rules. Do not duplicate them with broad app-wide
# keep rules unless a verified release-only failure requires it.
