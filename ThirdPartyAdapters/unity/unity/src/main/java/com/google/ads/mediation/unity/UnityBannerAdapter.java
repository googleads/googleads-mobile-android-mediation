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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorCode;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/**
 * The {@link UnityBannerAdapter} is used to load Unity Banner ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityBannerAdapter extends UnityMediationAdapter
        implements MediationBannerAdapter, BannerView.IListener{

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
    public void requestBannerAd(final Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                final AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {

        bannerListener = listener;

        String gameId = serverParameters.getString(KEY_GAME_ID);
        bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);
        if (!isValidIds(gameId, bannerPlacementId)) {
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityBannerAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (context == null || !(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            if (bannerListener != null) {
                bannerListener.onAdFailedToLoad(UnityBannerAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        Activity activity = (Activity) context;

        // Even though we are a banner request, we still need to initialize UnityAds.

        UnityInitializer.getInstance().initializeUnityAds(activity, gameId, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized");
                    float density = context.getResources().getDisplayMetrics().density;
                    int bannerWidth = Math.round(adSize.getWidthInPixels(context) / density);
                    int bannerHeight = Math.round(adSize.getHeightInPixels(context) / density);

                    UnityBannerSize size = new UnityBannerSize(bannerWidth, bannerHeight);

                    if (mBannerView == null){
                        mBannerView = new BannerView((Activity)context, bannerPlacementId, size);
                    }

                    mBannerView.setListener(UnityBannerAdapter.this);
                    mBannerView.load();
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
                    Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" + unityAdsInitializationError + "] " + s);
                    if (bannerListener != null) {
                        bannerListener.onAdFailedToLoad(UnityBannerAdapter.this,
                                AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    }
                }
        });


    }


    @Override
    public View getBannerView() {
        return mBannerView;
    }

    @Override
    public void onBannerLoaded(BannerView bannerView) {
        Log.v(TAG, "Unity Ads Banner finished loading banner for placement: " + mBannerView.getPlacementId());
        if (bannerListener != null) {
            bannerListener.onAdLoaded(UnityBannerAdapter.this);
        }
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
        if (bannerListener != null) {
            bannerListener.onAdClicked(UnityBannerAdapter.this);
        }
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
        Log.w(TAG, "Unity Ads Banner encountered an error: " + bannerErrorInfo.errorMessage);
        if (bannerListener != null) {
            if (bannerErrorInfo.errorCode == BannerErrorCode.NO_FILL) {
                bannerListener.onAdFailedToLoad(UnityBannerAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            } else {
                bannerListener.onAdFailedToLoad(UnityBannerAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
        bannerListener.onAdLeftApplication(UnityBannerAdapter.this);
    }
}
