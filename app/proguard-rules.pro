# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# =========================
# Release(minify) 兼容配置
# =========================

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Gson (反射解析) ---
# 保留泛型签名与注解信息，避免反射/序列化在混淆后失效
-keepattributes Signature
-keepattributes *Annotation*

# 项目里的 API model 目前多数依赖“字段名 == JSON key”。
# 混淆会改字段名，导致 release 下解析出来全是 null/默认值。
-keep class com.youyuan.music.compose.api.model.** { *; }
-keep class com.youyuan.music.compose.api.apis.** { *; }

# Retrofit 接口避免被裁剪
-keep interface com.youyuan.music.compose.api.apis.** { *; }

# Hilt / Inject（兜底）
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }