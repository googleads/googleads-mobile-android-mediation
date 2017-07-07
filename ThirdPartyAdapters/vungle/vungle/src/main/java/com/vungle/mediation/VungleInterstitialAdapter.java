package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.publisher.AdConfig;

public class VungleInterstitialAdapter implements MediationInterstitialAdapter {

    private static final String TAG = VungleManager.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVunglePub;
    private AdConfig adConfig;
    private final String ID = "interstitial";
    private String placementForPlay;

    private final VungleListener vungleListener = new VungleListener() {
        @Override
        public void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdStart(String placement) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdAvailable() {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        }

        @Override
        void onInitialized(boolean isSuccess) {
            if (mMediationInterstitialListener != null) {
                if (!isSuccess){
                    mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, 0);
                } else {
                    loadAd();
                }
            }
        }

        @Override
        void onAdFail(String placement) {
            if (placement.equals(placementForPlay)) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                }
            }
        }
    };

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        String[] placements = mediationExtras.getStringArray(VungleExtrasBuilder.EXTRA_ALL_PLACEMENTS);
        if (placements == null || placements.length == 0) {
            Log.e(TAG, "Placements should be specified!");
            if (mediationInterstitialListener != null)
                mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, 0);
            return;
        }
        mMediationInterstitialListener = mediationInterstitialListener;
        String appId = serverParameters.getString("appid");
        mVunglePub = VungleManager.getInstance(appId, placements, context);

        placementForPlay = mVunglePub.findPlacemnt(mediationExtras);
        adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

        mVunglePub.addListener(ID, vungleListener);
        if (mVunglePub.isInitialized()) {
            loadAd();
        } else {
            vungleListener.setWaitingInit(true);
            mVunglePub.init();
        }
    }

    private void loadAd() {
        if (mVunglePub.isAdPlayable(placementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else {
            vungleListener.waitForAd(placementForPlay);
            mVunglePub.loadAd(placementForPlay);
        }
    }

    @Override
    public void showInterstitial() {
        mVunglePub.playAd(placementForPlay, adConfig, ID);
    }

    @Override
    public void onDestroy() {
        mVunglePub.removeListener(ID);
    }

    @Override
    public void onPause() {
        mVunglePub.onPause();
    }

    @Override
    public void onResume() {
        mVunglePub.onResume();
    }
}
