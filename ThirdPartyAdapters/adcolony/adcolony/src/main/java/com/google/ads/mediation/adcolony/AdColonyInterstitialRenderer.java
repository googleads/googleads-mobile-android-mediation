package com.google.ads.mediation.adcolony;

import android.content.Context;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;

public class AdColonyInterstitialRenderer extends AdColonyInterstitialListener implements
        MediationInterstitialAd {

    private String zoneID;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mAdLoadCallback;
    private AdColonyInterstitial adColonyInterstitial;

    AdColonyInterstitialRenderer(String zoneID) {
        this.zoneID = zoneID;
    }

    public void requestInterstitial(
            final MediationAdLoadCallback<MediationInterstitialAd,
                    MediationInterstitialAdCallback> callback) {
        this.mAdLoadCallback = callback;
        AdColony.requestInterstitial(zoneID, this);
    }

    @Override
    public void showAd(Context context) {
        adColonyInterstitial.show();
    }

    @Override
    public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
        AdColonyInterstitialRenderer.this.adColonyInterstitial = adColonyInterstitial;
        mInterstitialAdCallback = mAdLoadCallback.onSuccess(AdColonyInterstitialRenderer.this);
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        mAdLoadCallback.onFailure("Failed to load ad.");
    }

    @Override
    public void onLeftApplication(AdColonyInterstitial ad) {
        super.onLeftApplication(ad);

        mInterstitialAdCallback.reportAdClicked();
        mInterstitialAdCallback.onAdLeftApplication();
    }

    @Override
    public void onOpened(AdColonyInterstitial ad) {
        super.onOpened(ad);

        mInterstitialAdCallback.onAdOpened();
        mInterstitialAdCallback.reportAdImpression();
    }

    @Override
    public void onClosed(AdColonyInterstitial ad) {
        super.onClosed(ad);

        mInterstitialAdCallback.onAdClosed();
    }

    @Override
    public void onExpiring(AdColonyInterstitial ad) {
        super.onExpiring(ad);

        AdColony.requestInterstitial(ad.getZoneID(), this);
    }
}

