package com.google.ads.mediation.adcolony;

import android.view.View;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class AdColonyBannerRenderer extends AdColonyAdViewListener implements MediationBannerAd {

    private final String requestedZone;
    private final AdColonyAdSize adSize;
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback;
    private MediationBannerAdCallback mAdLoadCallback;
    private AdColonyAdView adColonyAdView;

    public AdColonyBannerRenderer(MediationBannerAdConfiguration adConfiguration) {
       requestedZone = adConfiguration.getServerParameters().getString(AdColonyAdapterUtils.KEY_ZONE_ID);
        // Convert requested size to AdColony Ad Size.
        this.adSize = AdColonyAdapterUtils.adColonyAdSizeFromAdMobAdSize(
                adConfiguration.getContext(), adConfiguration.getAdSize());
    }

    @Override
    public void onRequestFilled(AdColonyAdView adColonyAdView) {
        this.adColonyAdView = adColonyAdView;
        this.mAdLoadCallback = mediationAdLoadCallback.onSuccess(this);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        this.mediationAdLoadCallback.onFailure("Failed to load banner.");
    }

    @Override
    public void onLeftApplication(AdColonyAdView ad) {
        this.mAdLoadCallback.onAdLeftApplication();
    }

    @Override
    public void onClosed(AdColonyAdView ad) {
        this.mAdLoadCallback.onAdClosed();
    }

    @Override
    public void onOpened(AdColonyAdView ad) {
        this.mAdLoadCallback.onAdOpened();
    }

    @Override
    public void onClicked(AdColonyAdView ad) {
        this.mAdLoadCallback.reportAdClicked();
    }

    @NonNull
    @Override
    public View getView() {
        return this.adColonyAdView;
    }

    public void requestBanner(MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
        if (adSize != null) {
            this.mediationAdLoadCallback = mediationAdLoadCallback;
            AdColony.requestAdView(requestedZone, this, adSize);
        } else {
            mediationAdLoadCallback.onFailure("Failed to request banner with unsupported size");
        }
    }

}
