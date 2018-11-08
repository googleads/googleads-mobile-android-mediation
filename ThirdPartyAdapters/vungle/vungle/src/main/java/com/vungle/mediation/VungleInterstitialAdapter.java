package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
public class VungleInterstitialAdapter implements MediationInterstitialAdapter {

    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private static final String mId = "interstitial";
    private static int sCounter = 0;
    private String mAdapterId;
    private String mPlacementForPlay;

    private final VungleListener mVungleListener = new VungleListener() {
        @Override
        public void onAdEnd(String placement,
                            boolean wasSuccessfulView,
                            boolean wasCallToActionClicked) {
            if (mMediationInterstitialListener != null) {
                if (wasCallToActionClicked) {
                    // Only the call to action button is clickable for Vungle ads. So the
                    // wasCallToActionClicked can be used for tracking clicks.
                    mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
                    mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
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
        void onInitialized(boolean isSuccess) {
            if (mMediationInterstitialListener != null) {
                if (!isSuccess) {
                    mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                } else {
                    loadAd();
                }
            }
        }

        @Override
        void onAdFail(String placement) {
            if (placement.equals(mPlacementForPlay)) {
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
        try {
            AdapterParametersParser.Config config =
                    AdapterParametersParser.parse(mediationExtras, serverParameters);
            mMediationInterstitialListener = mediationInterstitialListener;
            mVungleManager =
                    VungleManager.getInstance(config.getAppId());

            mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
            if(mPlacementForPlay != null && !mPlacementForPlay.isEmpty()) {
                mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

                mAdapterId = mId + String.valueOf(sCounter);
                sCounter++;

                mVungleManager.addListener(mAdapterId, mVungleListener);
                if (mVungleManager.isInitialized()) {
                    loadAd();
                } else {
                    mVungleListener.setWaitingInit(true);
                    mVungleManager.init(context);
                }
            } else {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        } catch (IllegalArgumentException e) {
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleListener.waitForAd(mPlacementForPlay);
            mVungleManager.loadAd(mPlacementForPlay);
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    @Override
    public void showInterstitial() {
        if (mVungleManager != null)
            mVungleManager.playAd(mPlacementForPlay, mAdConfig, mAdapterId);
    }

    @Override
    public void onDestroy() {
        if (mVungleManager != null)
            mVungleManager.removeListener(mAdapterId);
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }
}
