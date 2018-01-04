# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/baidu/Library/Android/sdk/tools/proguard/proguard-android.txt
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
-keep public class * extends android.app.Activity

-keep public class * extends android.app.Application

-keep public class * extends android.content.ContentProvider

-keep public class * extends android.content.BroadcastReceiver

 -keep class com.dianxinos.DXStatService.stat.TokenManager {
 public static java.lang.String getToken(android.content.Context);
 }

 -keepnames @com.google.android.gms.common.annotation.KeepName class *
 -keepclassmembernames class * {
         @com.google.android.gms.common.annotation.KeepName *;}
 -keep class com.google.android.gms.common.GooglePlayServicesUtil {
       public <methods>;}
 -keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
       public <methods>;}
 -keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
       public <methods>;}

