// Copyright 2016 Google Inc.
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

package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;

import com.unity3d.ads.UnityAds;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter
        implements MediationInterstitialAdapter, MediationBannerAdapter, OnContextChangedListener {

    /**
     * Mediation interstitial listener used to forward events from {@link UnitySingleton} to
     * Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mMediationInterstitialListener;

    /**
     * Placement ID used to determine what type of ad to load.
     */
    private String mPlacementId;

    /**
     * Placement ID for banner if requested.
     */
    private String bannerPlacementId;

    /**
     * The view for the banner instance.
     */
    private View bannerView;

    /**
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener bannerListener;
    
    /**
     * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
     * Ads SDK.
     */
    private final UnityAdapterDelegate mUnityAdapterDelegate = new UnityAdapterDelegate() {

        @Override
        public String getPlacementId() {
            return mPlacementId;
        }

        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
            // adapter is currently loading ads.
            if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started playing. Google Mobile Ads SDK does not support
            // callbacks for Interstitial ads when they start playing.
        }

        @Override
        public void onUnityAdsClick(String s) {
            // Unity Ads ad clicked.
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClicked(UnityAdapter.this);
                // Unity Ads doesn't provide a "leaving application" event, so assuming that the
                // user is leaving the application when a click is received, forwarding an on ad
                // left application event.
                mMediationInterstitialListener.onAdLeftApplication(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
            // This callback is not forwarded to the adapter by the UnitySingleton and the
            // adapter should use the onUnityAdsReady and onUnityAdsError callbacks to forward
            // Unity Ads SDK state to Google Mobile Ads SDK.
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            // Unity Ads ad closed.
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // Send Ad Failed to load event.
            if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
                Log.e(TAG, "Failed to load Interstitial ad from Unity Ads: " +
                        unityAdsError.toString());
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_NO_FILL);
            }

            if ((placementId.equals(bannerPlacementId)) && bannerListener != null) {
                Log.e(TAG, "Failed to load Banner ad from Unity Ads: " + unityAdsError.toString());
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_NO_FILL);
            }
        }
    };

    private final UnityAdapterBannerDelegate bannerDelegate = new UnityAdapterBannerDelegate() {
        @Override
        public String getPlacementId() {
            return bannerPlacementId;
        }

        @Override
        public void onUnityBannerLoaded(String placementId, View view) {
            // Unity Ads Banner ad has been loaded and is ready to be shown.
            bannerView = view;
            if (bannerListener != null) {
                bannerListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityBannerUnloaded(String placementId) {
            // Unity Ads Banner ad has been unloaded and is to be destroyed.
            bannerView = null;
        }

        @Override
        public void onUnityBannerShow(String placementId) {
            // Unity Ads Banner ad is visible to the user.
            if (bannerListener != null) {
                bannerListener.onAdOpened(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityBannerClick(String placementId) {
            // Unity Ads Banner ad has been clicked.
            if (bannerListener != null) {
                bannerListener.onAdClicked(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityBannerHide(String placementId) {
            // Unity Ads Banner ad is hidden from the user.
            if (bannerListener != null) {
                bannerListener.onAdClosed(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityBannerError(String message) {
            // Unity Ads SDK encountered an error.
            Log.w(TAG, "Failed to load Banner ad from Unity Ads: " + message);
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    };

    /**
     * Checks whether or not the provided Unity Ads IDs are valid.
     *
     * @param gameId      Unity Ads Game ID to be verified.
     * @param placementId Unity Ads Placement ID to be verified.
     * @return {@code true} if all the IDs provided are valid.
     */
    private static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, ids + " cannot be empty.");

            return false;
        }

        return true;
    }

    /**
     * Unity Ads requires an Activity context to Initialize. This method will return false if
     * the context provided is either null or is not an Activity context.
     *
     * @param context to be checked if it is valid.
     * @return {@code true} if the context provided is valid, {@code false} otherwise.
     */
    private static boolean isValidContext(Context context) {
        if (context == null) {
            Log.w(TAG, "Context cannot be null.");
            return false;
        }

        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            return false;
        }
        return true;
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        mMediationInterstitialListener = mediationInterstitialListener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, mPlacementId)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);

        // Check if the Unity Ads initialized successfully.
        if (UnityAds.isInitialized()) {
            // Unity Ads initialized successfully, request UnitySingleton to load an ad.
            UnitySingleton.loadAd(mUnityAdapterDelegate);
        } else {
            UnitySingleton.initializeUnityAds(mUnityAdapterDelegate,
                    (Activity) context, gameId, mPlacementId);
        }
    }

    @Override
    public void showInterstitial() {
        // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
        // ad.
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);

        if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
            // Request UnitySingleton to show interstitial ads.
            UnitySingleton.showAd(mUnityAdapterDelegate, mActivityWeakReference.get());
        } else {
            Log.w(TAG, "Failed to show Unity Ads Interstitial.");
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        }
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {
        bannerListener = listener;

        AdSize supportedSize = getSupportedAdSize(context, adSize);
        if (supportedSize == null) {
            Log.e(TAG, "Invalid ad size requested: " + adSize);
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        String gameId = serverParameters.getString(KEY_GAME_ID);
        bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, bannerPlacementId)) {
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        // Even though we are a banner request, we still need to initialize UnityAds.
        // Check if the Unity Ads initialized successfully.
        if (UnityAds.isInitialized()) {
            // Storing a weak reference to the Activity.
            mActivityWeakReference = new WeakReference<>((Activity) context);
            UnitySingleton.loadBannerAd(bannerDelegate);
        } else {
            UnitySingleton.initializeUnityAds(mUnityAdapterDelegate, (Activity) context,
                    gameId, bannerPlacementId, bannerDelegate);
        }
    }

    @Override
    public View getBannerView() {
        return bannerView;
    }

    AdSize getSupportedAdSize(Context context, AdSize adSize) {
        AdSize original = new AdSize(adSize.getWidth(), adSize.getHeight());

        ArrayList<AdSize> potentials = new ArrayList<>(2);
        potentials.add(AdSize.BANNER);
        potentials.add(AdSize.LEADERBOARD);

        return findClosestSize(context, original, potentials);
    }

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size.
     * Returns null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
          Context context, AdSize original, ArrayList<AdSize> potentials) {
       if (potentials == null || original == null) {
           return null;
       }
       float density = context.getResources().getDisplayMetrics().density;
       int actualWidth = Math.round(original.getWidthInPixels(context)/density);
       int actualHeight = Math.round(original.getHeightInPixels(context)/density);
       original = new AdSize(actualWidth, actualHeight);
        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
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

    private static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
          return false;
        }

        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth ||
            originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight ||
            originalHeight < potentialHeight) {
            return false;
        }

        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
      int area1 = size1.getWidth() * size1.getHeight();
      int area2 = size2.getWidth() * size2.getHeight();
      return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK
}
