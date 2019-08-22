package com.google.ads.mediation.facebook.rtb;

import android.content.Context;
import android.os.Bundle;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import  com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

import java.util.concurrent.atomic.AtomicBoolean;


public class FacebookRtbInterstitialAd implements MediationInterstitialAd, InterstitialAdExtendedListener {
    private MediationInterstitialAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback;
    private InterstitialAd interstitialAd;
    private MediationInterstitialAdCallback mInterstitalAdCallback;
    private AtomicBoolean didInterstitialAdClose = new AtomicBoolean();

    public FacebookRtbInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
                                     MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void render() {
        Bundle serverParameters = adConfiguration.getServerParameters();
        String placementId = FacebookMediationAdapter.getPlacementID(serverParameters);
        if (placementId == null || placementId.isEmpty()) {
            callback.onFailure("FacebookRtbInterstitialAd received a null or empty placement ID.");
            return;
        }
        interstitialAd = new InterstitialAd(adConfiguration.getContext(), placementId);
        interstitialAd.setAdListener(this);
        interstitialAd.loadAdFromBid(adConfiguration.getBidResponse());
    }

    @Override
    public void showAd(Context context) {
        if (interstitialAd.isAdLoaded()) {
            interstitialAd.show();
        }
    }

    @Override
    public void onInterstitialDisplayed(Ad ad) {
        if (mInterstitalAdCallback != null) {
            mInterstitalAdCallback.onAdOpened();
        }
    }

    @Override
    public void onInterstitialDismissed(Ad ad) {
        if (!didInterstitialAdClose.getAndSet(true) && mInterstitalAdCallback != null) {
            mInterstitalAdCallback.onAdClosed();
        }
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        callback.onFailure(adError.getErrorMessage());
    }

    @Override
    public void onAdLoaded(Ad ad) {
        mInterstitalAdCallback = callback.onSuccess(this);
    }

    @Override
    public void onAdClicked(Ad ad) {
        if (mInterstitalAdCallback != null) {
            // TODO: Upon approval, add this callback back in.
            // mInterstitalAdCallback.reportAdClicked();
            mInterstitalAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        if (mInterstitalAdCallback != null) {
            // TODO: Upon approval, add this callback back in.
            // mInterstitalAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onInterstitialActivityDestroyed() {
        if (!didInterstitialAdClose.getAndSet(true) && mInterstitalAdCallback != null) {
            mInterstitalAdCallback.onAdClosed();
        }
    }

    @Override
    public void onRewardedAdCompleted() {
        //no-op
    }

    @Override
    public void onRewardedAdServerSucceeded() {
        //no-op
    }

    @Override
    public void onRewardedAdServerFailed() {
        //no-op
    }
}
