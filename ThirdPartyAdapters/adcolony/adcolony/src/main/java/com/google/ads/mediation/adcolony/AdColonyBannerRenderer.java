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
    private MediationBannerAdCallback mBannerAdCallback;
    private AdColonyAdView adColonyAdView;

    public AdColonyBannerRenderer(MediationBannerAdConfiguration adConfiguration) {
       requestedZone = adConfiguration.getServerParameters().getString(AdColonyAdapterUtils.KEY_ZONE_ID);
        // Setting the requested size as it is as adcolony view size.
        this.adSize = new AdColonyAdSize(adConfiguration.getAdSize().getWidth(),adConfiguration.getAdSize().getHeight());
    }

    @Override
    public void onRequestFilled(AdColonyAdView adColonyAdView) {
        this.adColonyAdView = adColonyAdView;
        this.mBannerAdCallback = mediationAdLoadCallback.onSuccess(this);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        this.mediationAdLoadCallback.onFailure("Failed to load banner.");
    }

    @Override
    public void onLeftApplication(AdColonyAdView ad) {
        this.mBannerAdCallback.onAdLeftApplication();
    }

    @Override
    public void onClosed(AdColonyAdView ad) {
        this.mBannerAdCallback.onAdClosed();
    }

    @Override
    public void onOpened(AdColonyAdView ad) {
        this.mBannerAdCallback.onAdOpened();
    }

    @Override
    public void onClicked(AdColonyAdView ad) {
        this.mBannerAdCallback.reportAdClicked();
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
