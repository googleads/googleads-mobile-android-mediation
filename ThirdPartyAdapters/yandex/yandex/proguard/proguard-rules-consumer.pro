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