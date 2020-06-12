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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter
        implements MediationInterstitialAdapter, IUnityAdsExtendedListener, IUnityAdsLoadListener {

    /**
     * Mediation interstitial listener used to forward events to
     * Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mMediationInterstitialListener;

    /**
     * Placement ID used to determine what type of ad to load.
     */
    private String mPlacementId;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

    /**
     * Returns the placement ID of the ad being loaded
     *
     * @return mPlacementId
     */
    public String getPlacementId() {
        return mPlacementId;
    }

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

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        Log.v(TAG, "requesting interstitial ad");
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

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        Activity activity = (Activity) context;
        mActivityWeakReference = new WeakReference<>(activity);

        UnityInitializer.getInstance().initializeUnityAds(activity, gameId,
                new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized");
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError
                                                       unityAdsInitializationError, String s) {
                Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" +
                        unityAdsInitializationError + "] " + s);
            }
        });

        loadInterstitialAd(mPlacementId);
    }

    /**
     * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     *
     * @param placementId Used to forward Unity Ads events to the adapter.
     */
    protected void loadInterstitialAd(String placementId) {

        UnityAds.load(placementId, UnityAdapter.this);

    }


    /**
     * This method will show a Unity Ad
     */
    @Override
    public void showInterstitial() {

        // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
        // ad.
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);

        if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {

            // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
            // Unity Ads fails to shown an ad).
            UnityAds.show(mActivityWeakReference.get(), mPlacementId);

        } else {
            Log.w(TAG, "Failed to show Unity Ads Interstitial.");
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onUnityAdsReady(String placementId) {
        // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
        // adapter is currently loading ads.
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
        // This callback is not forwarded to the adapter and the
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
        // Unity Ad failed to show
        if (placementId.equals(getPlacementId()))
        {
            Log.e(TAG, "Failed to show Interstitial ad from Unity Ads: " +
                    unityAdsError.toString());
        }

        // check with google if we need to make any calls with mMediationInterstitialListener
    }

    @Override
    public void onUnityAdsAdLoaded(String s) {
        Log.d(UnityAdapter.TAG, "Ad successfully loaded " + s);
        mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
    }

    @Override
    public void onUnityAdsFailedToLoad(String s) {
        Log.e(UnityAdapter.TAG, "Ad load failure " + s);
        mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                AdRequest.ERROR_CODE_INTERNAL_ERROR);
    }
}
