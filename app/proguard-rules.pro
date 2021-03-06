# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/martin/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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


# AndBasx
-keep class com.pepperonas.jbasx.** { *; }
-dontwarn com.pepperonas.jbasx.**
-keep class com.pepperonas.andbasx.** { *; }
-dontwarn com.pepperonas.andbasx.**

# AesPrefs
-keep class com.pepperonas.aesprefs.** { *; }
-dontwarn com.pepperonas.aesprefs.**

# MaterialDialog
-keep class com.pepperonas.materialdialog.** { *; }
-dontwarn com.pepperonas.materialdialog.**

# Android Iconics
-keep class .R
-keep class **.R$* {
    <fields>;
}