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
        implements MediationInterstitialAdapter, MediationBannerAdapter, OnContextChangedListener, BannerView.IListener {

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
     * Callback object for Google's Banner Lifecycle.
     */
    private MediationBannerListener mBannerListener;

    private BannerView mBannerView;
    
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
        mActivityWeakReference = new WeakReference<>((Activity) context);

        // Make sure Unity Ads is initialized
        if(!UnityAds.isInitialized()) {
            UnitySingleton.initializeUnityAds(mUnityAdapterDelegate,
                    (Activity) context, gameId);
        }

        if (UnityAds.isReady(mPlacementId)){
            // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
            }
        }

        // Always send a load event on request, even if Unity is ready. This is important to Unity's reporting
        UnitySingleton.loadAd(mUnityAdapterDelegate);
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
    public void onDestroy() {
        if (mBannerView != null) {
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

        mBannerListener = listener;
        String gameId = serverParameters.getString(KEY_GAME_ID);
        String bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, bannerPlacementId)) {
            if (mBannerListener != null) {
                mBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            if (mBannerListener != null) {
                mBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (adSize == null) {
            Log.w(TAG, "Fail to request banner ad, adSize is null");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        if(mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }

        Integer bannerWidth = adSize.getWidth() < 320 ? 320 : adSize.getWidth();
        Integer bannerHeight = adSize.getHeight() < 50 ? 50 : adSize.getHeight();
        UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);
        mBannerView = new BannerView((Activity)context, bannerPlacementId, size);
        mBannerView.setListener(this);
        mBannerView.load();
    }

    // Banner Callbacks

    @Override
    public View getBannerView() {
        return mBannerView;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        mBannerView = bannerView;
        String info = "[w: " + bannerView.getSize().getWidth() + ", h: " + bannerView.getSize().getHeight() + "] and placement: " + bannerView.getPlacementId();
        Log.w(TAG, "Loaded banner ad with size " + info);

        if (mBannerListener != null) {
            mBannerListener.onAdLoaded(this);
        } else {
            Log.w(TAG, "Loaded banner for placement" + bannerView.getPlacementId() + " but Admob Listener is NULL");
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        mBannerListener.onAdClicked(this);
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
        if (bannerErrorInfo.errorCode == BannerErrorCode.NO_FILL) {
            Log.w(TAG, "Banner Ad failed to load - NO FILL");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
        } else {
            Log.w(TAG, "Banner Ad failed to load");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NETWORK_ERROR);
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        mBannerListener.onAdLeftApplication(this);
    }
}
