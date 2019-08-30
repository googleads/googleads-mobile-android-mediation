package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
public class VungleInterstitialAdapter
        implements MediationInterstitialAdapter, VungleInitializer.VungleInitializationListener {

    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private static final String mId = "interstitial";
    private static int sCounter = 0;
    private String mAdapterId;
    private String mPlacement;

    private final VungleListener mVungleListener = new VungleListener() {
        @Override
        public void onAdEnd(String placement,
                            boolean wasSuccessfulView,
                            boolean wasCallToActionClicked) {
            if (mMediationInterstitialListener != null) {
                if (wasCallToActionClicked) {
                    // Only the call to action button is clickable for Vungle ads. So the
                    // wasCallToActionClicked can be used for tracking clicks.
                    mMediationInterstitialListener.onAdClicked(
                            VungleInterstitialAdapter.this);
                    mMediationInterstitialListener.onAdLeftApplication(
                            VungleInterstitialAdapter.this);
                }
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
        void onAdFailedToLoad() {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                        AdRequest.ERROR_CODE_NO_FILL);
            }
        }

        @Override
        void onAdFail(String placement) {
            if (placement.equals(mPlacement) && mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
            }
        }
    };

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle", e);
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;
        mVungleManager = VungleManager.getInstance();

        mPlacement = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacement)) {
            Log.w(VungleMediationAdapter.TAG,
                    "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

        mAdapterId = mId + String.valueOf(sCounter);
        sCounter++;
        mVungleManager.addListener(mAdapterId, mVungleListener);
        if (!VungleInitializer.getInstance().isInitialized()) {
            VungleInitializer.getInstance().initialize(config.getAppId(),
                    context.getApplicationContext(), VungleInterstitialAdapter.this);
        } else {
            loadAd();
        }
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(mPlacement)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacement)) {
            mVungleListener.waitForAd(mPlacement);
            mVungleManager.loadAd(mPlacement);
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    @Override
    public void showInterstitial() {
        if (mVungleManager != null) {
            mVungleManager.playAd(mPlacement, mAdConfig, mAdapterId);
        }
    }

    @Override
    public void onInitializeSuccess() {
        loadAd();
    }

    @Override
    public void onInitializeError(String errorMessage) {
        Log.w(VungleMediationAdapter.TAG, "Failed to load ad from Vungle: " + errorMessage);
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void onDestroy() {
        if (mVungleManager != null) {
            mVungleManager.removeListener(mAdapterId);
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }
}
