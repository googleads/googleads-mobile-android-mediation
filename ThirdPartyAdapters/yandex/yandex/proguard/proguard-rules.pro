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

-keep public class com.google.ads.mediation.yandex.YandexMediationAdapter {
    !private <methods>;
}

-keepclassmembers public class com.google.ads.mediation.yandex.banner.BannerAdapterEventListener {
    public void onImpression(com.yandex.mobile.ads.common.ImpressionData);
}

-keepclassmembers public class com.google.ads.mediation.yandex.interstitial.InterstitialAdapterEventListener {
    public void onImpression(com.yandex.mobile.ads.common.ImpressionData);
}

-keepclassmembers public class com.google.ads.mediation.yandex.rewarded.RewardedAdapterEventListener {
    public void onImpression(com.yandex.mobile.ads.common.ImpressionData);
}

-keepclassmembers public class com.google.ads.mediation.yandex.nativeads.NativeAdapterEventListener {
    public void onImpression(com.yandex.mobile.ads.common.ImpressionData);
}

-keep public class com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdAsset {
    public <fields>;
}

-classobfuscationdictionary dictionary.txt