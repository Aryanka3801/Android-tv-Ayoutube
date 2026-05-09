# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# RxJava
-dontwarn io.reactivex.rxjava3.**

# Keep VideoItem (serialized via Intent)
-keep class com.example.myapp.model.VideoItem { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
