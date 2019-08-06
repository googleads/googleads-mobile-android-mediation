package com.google.ads.mediation.facebook.rtb;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdView;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;


public class FacebookRtbBannerAd implements MediationBannerAd, AdListener {

    private MediationBannerAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
    private AdView adView;
    private MediationBannerAdCallback mBannerAdCallback;

    public FacebookRtbBannerAd(MediationBannerAdConfiguration adConfiguration,
                               MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void render() {
        Bundle serverParameters = adConfiguration.getServerParameters();
        String placementId = FacebookMediationAdapter.getPlacementID(serverParameters);
        if (placementId == null || placementId.isEmpty()) {
            callback.onFailure("FacebookRtbBannerAd received a null or empty placement ID.");
            return;
        }
        try {
            adView = new AdView(adConfiguration.getContext(), placementId, adConfiguration.getBidResponse());
            adView.setAdListener(this);
            adView.loadAdFromBid(adConfiguration.getBidResponse());
        } catch (Exception e) {
            callback.onFailure("FacebookRtbBannerAd Failed to load: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public View getView() {
        return adView;
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        callback.onFailure(adError.getErrorMessage());
    }

    @Override
    public void onAdLoaded(Ad ad) {
        mBannerAdCallback = callback.onSuccess(this);
    }

    @Override
    public void onAdClicked(Ad ad) {
        if (mBannerAdCallback != null) {
            // TODO: Upon approval, add this callback back in.
            //mBannerAdCallback.reportAdClicked();
            mBannerAdCallback.onAdOpened();
            mBannerAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        if (mBannerAdCallback != null) {
            // TODO: Upon approval, add this callback back in.
            //mBannerAdCallback.reportAdImpression();
        }
    }
}
