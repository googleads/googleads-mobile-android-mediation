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
import android.support.annotation.Keep;
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
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import com.unity3d.ads.UnityAds;

import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter implements MediationRewardedVideoAdAdapter, MediationInterstitialAdapter,
        OnContextChangedListener, MediationBannerAdapter {
    public static final String TAG = UnityAdapter.class.getSimpleName();

    /**
     * Key to obtain Game ID, required for loading Unity Ads.
     */
    private static final String KEY_GAME_ID = "gameId";

    /**
     * Key to obtain Placement ID, used to set the type of ad to be shown. Unity Ads has changed
     * the name from Zone ID to Placement ID in Unity Ads SDK 2.0.0. To maintain backwards
     * compatibility the key is not changed.
     */
    private static final String KEY_PLACEMENT_ID = "zoneId";

    /**
     * Flag to keep track of whether or not the Unity rewarded video ad adapter has been
     * initialized.
     */
    private boolean mIsRewardedVideoAdAdapterInitialized;

    /**
     * Flag to determine whether or not the adapter is loading ads.
     */
    private boolean mIsLoading;

    /**
     * Mediation interstitial listener used to forward events from {@link UnitySingleton} to
     * Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mMediationInterstitialListener;

    /**
     * Mediation rewarded video ad listener used to forward events from {@link UnitySingleton}
     * to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    /**
     * Placement ID used to determine what type of ad to load.
     */
    private String mPlacementId;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

    /**
     * Placement ID for banner if requested.
     */
    private String mBannerPlacementId;

    /**
     * The view for the banner instance. UnityAds only supports one banner.
     */
    private View mBannerView;

    /**
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener mBannerListener;

    /**
     * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
     * Ads SDK.
     */
    private UnityAdapterDelegate mUnityAdapterDelegate = new UnityAdapterDelegate() {
        @Override
        public String getPlacementId() {
            return mPlacementId;
        }

        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
            // adapter is currently loading ads.
            if (mIsLoading) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
                    mIsLoading = false;
                } else if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdLoaded(UnityAdapter.this);
                    mIsLoading = false;
                }
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
            // video adapter.
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onVideoStarted(UnityAdapter.this);
            }
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
            } else if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdClicked(UnityAdapter.this);
                // Unity Ads doesn't provide a "leaving application" event, so assuming that the
                // user is leaving the application when a click is received, forwarding an on ad
                // left application event.
                mMediationRewardedVideoAdListener.onAdLeftApplication(UnityAdapter.this);
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
            } else if (mMediationRewardedVideoAdListener != null) {
                // Reward is provided only if the ad is watched completely.
                if (finishState == UnityAds.FinishState.COMPLETED) {
                    mMediationRewardedVideoAdListener.onVideoCompleted(
                            UnityAdapter.this);
                    // Unity Ads doesn't provide a reward value. The publisher is expected to
                    // override the reward in AdMob console.
                    mMediationRewardedVideoAdListener.onRewarded(
                            UnityAdapter.this, new UnityReward());
                }
                mMediationRewardedVideoAdListener.onAdClosed(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // Send Ad Failed to load event only if the adapter is currently loading ads.
            if (mIsLoading) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                            AdRequest.ERROR_CODE_NO_FILL);
                    mIsLoading = false;
                } else if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdFailedToLoad(UnityAdapter.this,
                            AdRequest.ERROR_CODE_NO_FILL);
                    mIsLoading = false;
                }
            }
        }
    };

    private UnityAdapterBannerDelegate mBannerDelegate = new UnityAdapterBannerDelegate() {
        @Override
        public String getPlacementId() {
            return mBannerPlacementId;
        }

        @Override
        public void onUnityBannerLoaded(String placementId, View view) {
            mBannerView = view;
            if (mBannerListener != null) {
                mBannerListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityBannerUnloaded(String placementId) {
            mBannerView = null;
        }

        @Override
        public void onUnityBannerShow(String placementId) {
            mBannerListener.onAdOpened(UnityAdapter.this);
        }

        @Override
        public void onUnityBannerClick(String placementId) {
            mBannerListener.onAdClicked(UnityAdapter.this);
        }

        @Override
        public void onUnityBannerHide(String placementId) {
            mBannerListener.onAdClosed(UnityAdapter.this);
        }

        @Override
        public void onUnityBannerError(String message) {
            mBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
            return;
        }

        // Check if the Unity Ads initialized successfully.
        if (!UnitySingleton.initializeUnityAds(mUnityAdapterDelegate, (Activity) context, gameId)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);

        // Unity Ads initialized successfully, request UnitySingleton to load ads for mPlacementId.
        mIsLoading = true;
        UnitySingleton.loadAd(mUnityAdapterDelegate);
    }

    @Override
    public void showInterstitial() {
        // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
        // ad.
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);

        Activity activity = mActivityWeakReference.get();
        if (activity == null) {
            // Activity is null, logging a warning and sending ad closed callback.
            Log.w(TAG, "An activity context is required to show Unity Ads, please call "
                    + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
            return;
        }

        // Request UnitySingleton to show interstitial ads.
        UnitySingleton.showAd(mUnityAdapterDelegate, activity);
    }

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, mPlacementId)) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            return;
        }

        // Check if the Unity Ads initialized.
        if (!UnitySingleton.initializeUnityAds(mUnityAdapterDelegate, (Activity) context, gameId)) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);

        mIsRewardedVideoAdAdapterInitialized = true;
        mMediationRewardedVideoAdListener.onInitializationSucceeded(this);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        // Request UnitySingleton to load ads for mPlacementId.
        mIsLoading = true;
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(serverParameters.getString(KEY_GAME_ID), mPlacementId)) {
            mMediationRewardedVideoAdListener.onAdFailedToLoad(
                    UnityAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        UnitySingleton.loadAd(mUnityAdapterDelegate);
    }

    @Override
    public void showVideo() {
        // Unity Ads does not have an ad opened callback. Sending ad opened before showing the ad.
        mMediationRewardedVideoAdListener.onAdOpened(UnityAdapter.this);

        Activity activity = mActivityWeakReference.get();
        if (activity == null) {
            // Activity is null, logging a warning and sending ad closed callback.
            Log.w(TAG, "An activity context is required to show Unity Ads, please call "
                    + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
            mMediationRewardedVideoAdListener.onAdClosed(UnityAdapter.this);
            return;
        }

        // Request UnitySingleton to show video ads.
        UnitySingleton.showAd(mUnityAdapterDelegate, activity);
    }

    @Override
    public boolean isInitialized() {
        return mIsRewardedVideoAdAdapterInitialized && UnityAds.isInitialized();
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onContextChanged(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Unity Ads requires an Activity context to show "
                    + "ads.");
            return;
        }

        // Storing a weak reference of the current Activity to be used when showing an ad.
        mActivityWeakReference = new WeakReference<>((Activity) context);
    }

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {
        mBannerListener = listener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        mBannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, mBannerPlacementId)) {
            if (mBannerListener != null) {
                mBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            return;
        }

        // Even though we are a banner request, we still need to initialize UnityAds.
        // Check if the Unity Ads initialized successfully.
        if (!UnitySingleton.initializeUnityAds(mUnityAdapterDelegate, (Activity) context, gameId)) {
            if (mBannerListener != null) {
                mBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);

        UnitySingleton.loadBannerAd(mBannerDelegate);
    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }
}