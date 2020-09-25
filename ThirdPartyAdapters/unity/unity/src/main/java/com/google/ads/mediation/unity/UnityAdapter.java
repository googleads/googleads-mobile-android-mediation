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

import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorCode;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter extends UnityMediationAdapter
        implements MediationInterstitialAdapter, MediationBannerAdapter, BannerView.IListener{

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
    private BannerView mBannerView;

    /**
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener bannerListener;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

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

        UnitySingleton.getInstance().initializeUnityAds(activity, gameId);
        UnitySingleton.getInstance().loadAd(mUnityAdapterDelegate);

    }

    @Override
    public void showInterstitial() {
        // Unity Ads does not have an ad opened callback. Sending Ad Opened event before showing the
        // ad.
        mMediationInterstitialListener.onAdOpened(UnityAdapter.this);

        if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
            // Request UnitySingleton to show interstitial ads.
            UnitySingleton.getInstance().showAd(mUnityAdapterDelegate, mActivityWeakReference.get());
        } else {
            Log.w(TAG, "Failed to show Unity Ads Interstitial.");
            mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
        }
    }

    @Override
    public void onDestroy() {
        if(mBannerView != null) {
            mBannerView.destroy();
        }
        mBannerView = null;
    }

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

        Log.v(TAG, "Requesting Unity Ads Banner");

        bannerListener = listener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, bannerPlacementId)) {
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        Activity activity = (Activity) context;

        // Even though we are a banner request, we still need to initialize UnityAds.
        UnitySingleton.getInstance().initializeUnityAds(activity, gameId);

        float density = context.getResources().getDisplayMetrics().density;
        int bannerWidth = Math.round(adSize.getWidthInPixels(context) / density);
        int bannerHeight = Math.round(adSize.getHeightInPixels(context) / density);

        UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);

        if (mBannerView == null){
            mBannerView = new BannerView((Activity)context, bannerPlacementId, size);
        }

        mBannerView.setListener(this);
        mBannerView.load();
    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        Log.v(TAG, "Unity Ads Banner finished loading banner for placement: " + mBannerView.getPlacementId());
        bannerListener.onAdLoaded(UnityAdapter.this);
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        bannerListener.onAdClicked(UnityAdapter.this);
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
        Log.w(TAG, "Unity Ads Banner encountered an error: " + bannerErrorInfo.errorMessage);
        if (bannerErrorInfo.errorCode == BannerErrorCode.NO_FILL) {
            bannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
        } else {
            bannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        bannerListener.onAdLeftApplication(UnityAdapter.this);
    }
}
