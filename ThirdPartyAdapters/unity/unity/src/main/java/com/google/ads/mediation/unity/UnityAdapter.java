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
public class UnityAdapter extends UnityMediationAdapter implements MediationInterstitialAdapter,
        IUnityAdsExtendedListener, MediationBannerAdapter {

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
     * UnityBannerAd instance.
     */
    private UnityBannerAd bannerAd;

    /**
     * IUnityAdsLoadListener instance.
     */
    private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String s) {
            Log.d(TAG, "Unity Ads interstitial ad successfully loaded for placement ID '"
                    + s + "'.");
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String s) {
            Log.e(TAG, "Unity Ads interstitial ad load failure for placement ID '"
                    + s + "'.");
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                            AdRequest.ERROR_CODE_NO_FILL);
            }
        }
    };

    /**
     * Returns the placement ID of the ad being loaded.
     *
     * @return mPlacementId.
     */
    private String getPlacementId() { return mPlacementId; }

    /**
     * Checks whether or not the provided Unity Ads IDs are valid.
     *
     * @param gameId      Unity Ads Game ID to be verified.
     * @param placementId Unity Ads Placement ID to be verified.
     * @return {@code true} if all the IDs provided are valid.
     */
    public static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, "Unity Ads failed to load interstitial ad: " + ids + " cannot be empty.");
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

        final String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, getPlacementId())) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Unity Ads failed to load interstitial ad for placement ID '" +
                    getPlacementId() + "': Context is not an Activity. Unity Ads requires an Activity" +
                    " context to load ads.");
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        Activity activity = (Activity) context;
        mActivityWeakReference = new WeakReference<>(activity);

        UnityInitializer.getInstance().initializeUnityAds(context, gameId,
                new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Log.d(TAG, "Unity Ads successfully initialized, " +
                        "can now load interstitial ad for placement ID '" + getPlacementId() +
                                "' in game '" + gameId + "'.");
                loadInterstitialAd(getPlacementId());
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError
                                                       unityAdsInitializationError, String s) {
                Log.e(TAG, "Unity Ads initialization failed: [" +
                        unityAdsInitializationError + "] " + s + ", cannot  load interstitial ad for " +
                        "placement ID '" + getPlacementId() + "' in game '" + gameId + "'");
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
            }
        });

    }

    /**
     * This method will load a Unity ad for the given Placement ID.
     *
     * @param placementId Used to identify the ad being loaded.
     */
    protected void loadInterstitialAd(String placementId) {

        UnityAds.load(placementId, mUnityLoadListener);

    }

    /**
     * This method will show a Unity Ad.
     */
    @Override
    public void showInterstitial() {

        // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
        // ad.
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);

        Activity activityReference = mActivityWeakReference == null ? null : mActivityWeakReference.get();
        if (activityReference != null) {
            if (UnityAds.isReady(getPlacementId())) {
                // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
                // Unity Ads fails to show an ad).
                UnityAds.show(activityReference, getPlacementId());
            } else {
                Log.w(TAG, "Unity Ads failed to show interstitial ad for placement ID '" + getPlacementId() +
                        "'. Placement is not ready.");
                mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
            }
        } else {
            Log.w(TAG, "Failed to show interstitial ad for placement ID '" + getPlacementId() +
                    "' from Unity Ads: Activity context is null.");
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        }
    }

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras){
        bannerAd = new UnityBannerAd();
        bannerAd.requestBannerAd(context, listener, serverParameters, adSize, adRequest, mediationExtras);
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        // Unity Ads is ready to show ads for the given placementId.
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        // Unity Ads video ad started playing. Google Mobile Ads SDK does not support
        // callbacks for Interstitial ads when they start playing.
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        Log.v(TAG, "Unity interstitial ad for placement ID '" + placementId + "' was clicked.");
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
        // This callback is not forwarded to Google Mobile Ads SDK. onUnityAdsError should be used
        // to forward Unity Ads SDK state to Google Mobile Ads SDK.
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {

        // Unity Ads ad closed.
        if (mMediationInterstitialListener != null) {
            if (finishState == UnityAds.FinishState.ERROR){
                Log.v(TAG, "Unity interstitial ad for placement ID '"
                        + placementId + "' finished with an error.");
            }
            else
            {
                Log.v(TAG, "Unity interstitial ad for placement ID '"
                        + placementId + "' finished playing.");
            }
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String errorMessage) {
        // Unity Ads ad failed to show.
        Log.e(TAG, "Failed to show interstitial ad for placement ID '" + getPlacementId() +
                "' from Unity Ads. Error: " + unityAdsError.toString() + " - " + errorMessage);
    }

    @Override
    public void onDestroy() {
        mMediationInterstitialListener = null;
        if (bannerAd != null)
        {
            bannerAd.onDestroy();
        }
    }

    @Override
    public void onPause() {
        if (bannerAd != null)
        {
            bannerAd.onPause();
        }
    }

    @Override
    public void onResume() {
        if (bannerAd != null)
        {
            bannerAd.onResume();
        }
    }

    @Override
    public View getBannerView() {
        if (bannerAd != null) {
            return bannerAd.getBannerView();
        }
        return null;
    }

}
