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
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mAdLoadCallback;
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
        this.mAdLoadCallback.onSuccess(this);
    }

    @Override
    public void onClicked(AdColonyAdView ad) {
        super.onClicked(ad);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        super.onRequestNotFilled(zone);
        this.mAdLoadCallback.onFailure("Failed to load banner.");
    }

    @NonNull
    @Override
    public View getView() {
        return this.adColonyAdView;
    }

    public void requestBanner(MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
        if (adSize != null) {
            this.mAdLoadCallback = mediationAdLoadCallback;
            AdColony.requestAdView(requestedZone, this, adSize);
        } else {
            mediationAdLoadCallback.onFailure("Failed to request banner with unsupported size");
        }
    }

}
