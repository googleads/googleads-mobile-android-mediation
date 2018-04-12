package com.applovin.mediation;

import android.content.Context;
import android.util.Log;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeListener;

import java.lang.ref.WeakReference;
import java.util.List;

public class AppLovinNativeAdListener
        implements AppLovinNativeAdLoadListener, AppLovinNativeAdPrecacheListener {

    private AppLovinNativeAdapter adapter;
    private MediationNativeListener nativeListener;
    private AppLovinSdk appLovinSdk;
    private WeakReference<Context> contextWeakReference;

    public AppLovinNativeAdListener(AppLovinNativeAdapter adapter,
                                    MediationNativeListener nativeListener,
                                    AppLovinSdk sdk,
                                    Context context) {
        this.adapter = adapter;
        this.nativeListener = nativeListener;
        this.appLovinSdk = sdk;
        contextWeakReference = new WeakReference<>(context);
    }

    @Override
    public void onNativeAdsLoaded(List<AppLovinNativeAd> nativeAds) {
        if (nativeAds.size() > 0 && isValidNativeAd(nativeAds.get(0))) {
            appLovinSdk.getNativeAdService()
                    .precacheResources(nativeAds.get(0), AppLovinNativeAdListener.this);
        } else {
            notifyAdFailure(AdRequest.ERROR_CODE_NO_FILL);
        }
    }

    @Override
    public void onNativeAdsFailedToLoad(final int errorCode) {
        notifyAdFailure(AppLovinUtils.toAdMobErrorCode(errorCode));
    }

    @Override
    public void onNativeAdImagesPrecached(AppLovinNativeAd ad) {
        // Create a native ad.
        if (contextWeakReference.get() == null) {
            Log.w(AppLovinNativeAdapter.TAG, "Failed to create mapper. Context is null.");
            notifyAdFailure(AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }
        final AppLovinNativeAdMapper mapper =
                new AppLovinNativeAdMapper(ad, contextWeakReference.get());
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nativeListener.onAdLoaded(adapter, mapper);
            }
        });
    }

    @Override
    public void onNativeAdVideoPreceached(AppLovinNativeAd ad) {
        // Do nothing.
    }

    @Override
    public void onNativeAdImagePrecachingFailed(AppLovinNativeAd ad, final int errorCode) {
        notifyAdFailure(AppLovinUtils.toAdMobErrorCode(errorCode));
    }

    @Override
    public void onNativeAdVideoPrecachingFailed(AppLovinNativeAd ad, final int errorCode) {
        notifyAdFailure(AppLovinUtils.toAdMobErrorCode(errorCode));
    }

    /**
     * Sends a failure callback to {@link #nativeListener}.
     * @param errorCode AdMob {@link AdRequest} error code.
     */
    private void notifyAdFailure(final int errorCode) {
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nativeListener.onAdFailedToLoad(adapter, errorCode);
            }
        });

    }

    /**
     * Checks whether or not the {@link AppLovinNativeAd} has all the required assets.
     * @param nativeAd AppLovin native ad.
     * @return {@code true} if the native ad has all the required assets.
     */
    private static boolean isValidNativeAd(AppLovinNativeAd nativeAd) {
        return nativeAd.isImagePrecached() && nativeAd.getImageUrl() != null
                && nativeAd.getIconUrl() != null && nativeAd.getTitle() != null
                && nativeAd.getDescriptionText() != null && nativeAd.getCtaText() != null;
    }
}
