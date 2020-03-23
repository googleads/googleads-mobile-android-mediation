// Copyright 2014 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;

import java.util.ArrayList;

import androidx.annotation.Keep;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter implements MediationInterstitialAdapter,
        MediationBannerAdapter {

    private static final String TAG = VungleInterstitialAdapter.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private String mPlacementForPlay;

    //banner/MREC
    private volatile RelativeLayout adLayout;
    private MediationBannerListener mMediationBannerListener;
    private VungleBannerAdapter mBannerRequest;

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
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;
        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacementForPlay)) {
            Log.w(TAG, "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadAd();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                        if (mMediationInterstitialListener != null) {
                            mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                }
        );
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mPlacementForPlay, new VungleListener() {
                @Override
                void onAdAvailable() {
                    mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
                }

                @Override
                void onAdFailedToLoad(int errorCode) {
                    mMediationInterstitialListener.onAdFailedToLoad(
                            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                }
            });
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
        if (mVungleManager != null)
            mVungleManager.playAd(mPlacementForPlay, mAdConfig, new VungleListener() {
                @Override
                void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                    if (mMediationInterstitialListener != null) {
                        if (wasCallToActionClicked) {
                            // Only the call to action button is clickable for Vungle ads. So the
                            // wasCallToActionClicked can be used for tracking clicks.
                            mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
                        }
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdStart(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdFail(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: "+ hashCode());
        if (mBannerRequest != null) {
            mBannerRequest.destroy(adLayout);
            mBannerRequest = null;
        }
        adLayout = null;
    }

    //banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        if (mBannerRequest != null) {
            mBannerRequest.updateVisibility(false);
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        if (mBannerRequest != null) {
            mBannerRequest.updateVisibility(true);
        }
    }

    @Override
    public void requestBannerAd(Context context,
                                final MediationBannerListener mediationBannerListener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        Log.d(TAG, "requestBannerAd");
        mMediationBannerListener = mediationBannerListener;
        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationBannerListener != null) {
                mediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mVungleManager = VungleManager.getInstance();

        String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
        Log.d(TAG, "requestBannerAd for Placement: " + placementForPlay + " ###  Adapter instance: " + this.hashCode());

        if (TextUtils.isEmpty(placementForPlay)) {
            String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        if (!hasBannerSizeAd(context, adSize, adConfig)) {
            String message = "Failed to load ad from Vungle: Invalid banner size.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        adLayout = new RelativeLayout(context);
        // Make adLayout wrapper match the requested ad size, as Vungle's ad uses MATCH_PARENT for
        // its dimensions.
        RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
                adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
        adLayout.setLayoutParams(adViewLayoutParams);

        mBannerRequest = mVungleManager.getBannerRequest(placementForPlay, config.getRequestUniqueId(), adConfig);
        if (mBannerRequest == null) {
            //Adapter does not support multiple Banner instances playing for same placement except for Refresh
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mBannerRequest.setAdLayout(adLayout);
        mBannerRequest.setVungleListener(mVungleBannerListener);
        mBannerRequest.requestBannerAd(context, config.getAppId());
    }

    private VungleListener mVungleBannerListener = new VungleListener() {
        @Override
        void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
            if (mMediationBannerListener != null) {
                if (wasCallToActionClicked) {
                    // Only the call to action button is clickable for Vungle ads. So the
                    // wasCallToActionClicked can be used for tracking clicks.
                    mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
                }
            }
        }

        @Override
        void onAdAvailable() {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        }

        @Override
        void onAdStart(String placement) {
            //let's load it again to mimic auto-cache, don't care about errors
            if (mBannerRequest != null) {
                mBannerRequest.preCache();
            }
        }

        @Override
        void onAdFail(String placement) {
            Log.w(TAG, "Ad playback error Placement: " + placement + ";" + mBannerRequest);
        }

        @Override
        void onAdFailedToLoad(int errorCode) {
            Log.w(TAG, "Failed to load ad from Vungle: " + errorCode + ";" + mBannerRequest);
            if(mMediationBannerListener!=null) {
                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, errorCode);
            }
        }
    };

    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView # instance: "+hashCode());
        return adLayout;
    }

    private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
        ArrayList<AdConfig.AdSize> potentials = new ArrayList<>(4);
        potentials.add(0, BANNER_SHORT);
        potentials.add(1, BANNER);
        potentials.add(2, BANNER_LEADERBOARD);
        potentials.add(3, VUNGLE_MREC);
        Log.i(TAG, "Potential ad sizes: " + potentials.toString());
        AdConfig.AdSize closestSize = findClosestSize(context, adSize, potentials);
        if (closestSize == null) {
            Log.i(TAG, "Not found closest ad size: " + adSize);
            return false;
        }
        Log.i(TAG, "Found closest ad size: " + closestSize.toString() + " for request ad size:" + adSize);

        adConfig.setAdSize(closestSize);

        return true;
    }

    // Copied some code from FB adapter:
    // https://github.com/googleads/googleads-mobile-android-mediation/blob/ebce3b3ccf1c7a0cd8ecb31819c0037b8885d584/
    // ThirdPartyAdapters/facebook/facebook/src/main/java/com/google/ads/mediation/facebook/FacebookAdapter.java#L760

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size. Returns
     * null if none are within given threshold size range.
     */
    private AdConfig.AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdConfig.AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
        original = new AdSize(actualWidth, actualHeight);

        AdConfig.AdSize largestPotential = null;
        for (AdConfig.AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    private static boolean isSizeInRange(AdSize original, AdConfig.AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdConfig.AdSize getLargerByArea(AdConfig.AdSize size1, AdConfig.AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK

}
