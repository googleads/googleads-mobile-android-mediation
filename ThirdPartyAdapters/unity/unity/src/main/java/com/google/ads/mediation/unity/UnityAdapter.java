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
        implements MediationInterstitialAdapter, MediationBannerAdapter {

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
    private MediationBannerListener mMediationBannerListener;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

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
            if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started playing.
            if (placementId.equals(getPlacementId()) && mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
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
            }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
            // Unity Ads SDK NO_FILL state to Google Mobile Ads SDK.
            if (mMediationInterstitialListener != null && placementId.equals(getPlacementId()) && newState.equals(UnityAds.PlacementState.NO_FILL)) {
                Log.e(TAG, "UnityAds no fill: " + placementId);
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            }
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            // Unity Ads ad closed.
            if (placementId.equals(getPlacementId())) {
                if (mMediationInterstitialListener != null) {
                    if (finishState == UnityAds.FinishState.ERROR) {
                        mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    } else {
                        mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
                    }
                }
                UnityAds.removeListener(mUnityAdapterDelegate);
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
        }
    };

    private BannerView.IListener mUnityBannerListener = new BannerView.Listener() {
        @Override
        public void onBannerLoaded(BannerView bannerAdView) {
            if (mMediationBannerListener != null) {
                Log.v(TAG, "Unity Ads Banner finished loading banner for placement: " + mBannerView.getPlacementId());
                mMediationBannerListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
            if (mMediationBannerListener != null) {
                Log.w(TAG, "Unity Ads Banner encountered an error: " + errorInfo.errorMessage);

                if (errorInfo.errorCode == BannerErrorCode.NO_FILL) {
                    mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                } else {
                    mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        }

        @Override
        public void onBannerClick(BannerView bannerAdView) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdClicked(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerLeftApplication(BannerView bannerAdView) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdLeftApplication(UnityAdapter.this);
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

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(Context context,
                                      final MediationInterstitialListener mediationInterstitialListener,
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

        UnityAds.addListener(mUnityAdapterDelegate);
        UnitySingleton.getInstance().initializeUnityAds(activity, gameId,
                new UnitySingleton.Listener() {
                    @Override
                    public void onInitializeSuccess() {
                        UnityAds.load(mPlacementId);
                    }

                    @Override
                    public void onInitializeError(String message) {
                        mediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    }
                });
    }

    @Override
    public void showInterstitial() {
        if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
           UnityAds.show(mActivityWeakReference.get(), mPlacementId);
        } else {
            Log.w(TAG, "Failed to show Unity Ads Interstitial.");
            mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }
    //endregion

    //region MediationAdapter implementation.
    @Override
    public void onDestroy() {
        if(mBannerView != null) {
            mBannerView.destroy();
        }
        mBannerView = null;
        UnityAds.removeListener(mUnityAdapterDelegate);
        mUnityAdapterDelegate = null;
        mUnityBannerListener = null;
        mMediationInterstitialListener = null;
        mMediationBannerListener = null;
    }

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}
    //endregion

    //region MediationBannerAdapter implementation.
    @Override
    public void requestBannerAd(Context context,
                                final MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {

        Log.v(TAG, "Requesting Unity Ads Banner");

        mMediationBannerListener = listener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, bannerPlacementId)) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        Activity activity = (Activity) context;


        float density = context.getResources().getDisplayMetrics().density;
        int bannerWidth = Math.round(adSize.getWidthInPixels(context) / density);
        int bannerHeight = Math.round(adSize.getHeightInPixels(context) / density);

        UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);
        if (mBannerView == null){
            mBannerView = new BannerView((Activity)context, bannerPlacementId, size);
        }

        mBannerView.setListener(mUnityBannerListener);

        UnitySingleton.getInstance().initializeUnityAds(activity, gameId,
                new UnitySingleton.Listener() {
                    @Override
                    public void onInitializeSuccess() {
                        mBannerView.load();
                    }

                    @Override
                    public void onInitializeError(String message) {
                        mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    }
                });
    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }
    //endregion
}
