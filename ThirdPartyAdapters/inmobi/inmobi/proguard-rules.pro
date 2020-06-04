# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${ANDROID_HOME}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Proguard configuration, as suggested by InMobi:
#   https://support.inmobi.com/monetize/android-guidelines

-keepattributes SourceFile,LineNumberTable
-keep class com.inmobi.** { *; }
-keep public class com.google.android.gms.**
-dontwarn com.google.android.gms.**
-dontwarn com.squareup.picasso.**
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{
     public *;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info{
     public *;
}

# skip the Picasso library classes
-keep class com.squareup.picasso.** {*;}
-dontwarn com.squareup.okhttp.**

# skip Moat classes
-keep class com.moat.** {*;}
-dontwarn com.moat.**

# skip IAB classes
-keep class com.iab.** {*;}
-dontwarn com.iab.**
