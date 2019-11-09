package com.jirbo.adcolony;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link AdColonyBannerAdListener} class is used to forward Banner ad
 * events from AdColony SDK to Google Mobile Ads SDK.
 */
class AdColonyBannerAdListener extends AdColonyAdViewListener {
    /**
     * The MediationBannerListener used to report callbacks.
     */
    private MediationBannerListener mediationBannerListener;
    /**
     * The AdColony banner adapter.
     */
    private AdColonyAdapter adapter;

    AdColonyBannerAdListener(@NonNull AdColonyAdapter adapter,
                             @NonNull MediationBannerListener listener) {
        mediationBannerListener = listener;
        this.adapter = adapter;
    }

    @Override
    public void onClicked(AdColonyAdView ad) {
        mediationBannerListener.onAdClicked(adapter);
    }

    @Override
    public void onOpened(AdColonyAdView ad) {
        mediationBannerListener.onAdOpened(adapter);
    }

    @Override
    public void onClosed(AdColonyAdView ad) {
        mediationBannerListener.onAdClosed(adapter);
    }

    @Override
    public void onLeftApplication(AdColonyAdView ad) {
        mediationBannerListener.onAdLeftApplication(adapter);
    }

    @Override
    public void onRequestFilled(AdColonyAdView adColonyAdView) {
        adapter.setAdView(adColonyAdView);
        mediationBannerListener.onAdLoaded(adapter);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        mediationBannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NO_FILL);
    }
}
