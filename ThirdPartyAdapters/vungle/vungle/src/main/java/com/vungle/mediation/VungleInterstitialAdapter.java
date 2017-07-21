package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.publisher.AdConfig;

public class VungleInterstitialAdapter implements MediationInterstitialAdapter {

    private static final String TAG = VungleManager.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig adConfig;
    private final String ID = "interstitial";
    private static int counter = 0;
    private String adapterId;
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
                    mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
        String[] placements;
        if (mediationExtras != null) {
            placements = mediationExtras.getStringArray(VungleExtrasBuilder.EXTRA_ALL_PLACEMENTS);
            if (placements == null || placements.length == 0) {
                Log.e(TAG, "Placements should be specified!");
                if (mediationInterstitialListener != null) {
                    mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
                return;
            }
        } else {
            Log.e(TAG, "mediationExtras is null.");
            if (mediationInterstitialListener != null)
                mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        mMediationInterstitialListener = mediationInterstitialListener;
        String appId = serverParameters.getString("appid");
        mVungleManager = VungleManager.getInstance(appId, placements);

        placementForPlay = mVungleManager.findPlacemnt(mediationExtras);
        adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

        adapterId = ID + String.valueOf(counter);
        counter++;

        mVungleManager.addListener(adapterId, vungleListener);
        if (mVungleManager.isInitialized()) {
            loadAd();
        } else {
            vungleListener.setWaitingInit(true);
            mVungleManager.init(context);
        }
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(placementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else {
            vungleListener.waitForAd(placementForPlay);
            mVungleManager.loadAd(placementForPlay);
        }
    }

    @Override
    public void showInterstitial() {
        mVungleManager.playAd(placementForPlay, adConfig, adapterId);
    }

    @Override
    public void onDestroy() {
        mVungleManager.removeListener(adapterId);
    }

    @Override
    public void onPause() {
        mVungleManager.onPause();
    }

    @Override
    public void onResume() {
        mVungleManager.onResume();
    }
}
