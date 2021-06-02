// Copyright 2020 Google Inc.
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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdapterError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/**
 * The {@link UnityBannerAd} is used to load Unity Banner ads and mediate the callbacks between
 * Google Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityBannerAd extends UnityMediationAdapter implements MediationBannerAdapter {

  /**
   * Placement ID for banner if requested.
   */
  private String bannerPlacementId;

  /**
   * Game ID, required for loading Unity Ads.
   */
  private String gameId;

  /**
   * The view for the banner instance.
   */
  private BannerView mBannerView;

  /**
   * Callback object for Google's Banner Lifecycle.
   */
  private MediationBannerListener mMediationBannerListener;

  /**
   * BannerView.IListener instance.
   */
  private BannerView.IListener mUnityBannerListener = new BannerView.Listener() {
    @Override
    public void onBannerLoaded(BannerView bannerView) {
      Log.v(TAG,
          "Unity Ads finished loading banner ad for placement ID '" + mBannerView.getPlacementId()
              + "'.");
      if (mMediationBannerListener == null) {
        return;
      }
      mMediationBannerListener.onAdLoaded(UnityBannerAd.this);
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
      Log.v(TAG,
          "Unity Ads banner for placement ID '" + mBannerView.getPlacementId() + "' was clicked.");
      if (mMediationBannerListener == null) {
        return;
      }

      mMediationBannerListener.onAdClicked(UnityBannerAd.this);
      mMediationBannerListener.onAdOpened(UnityBannerAd.this);
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
      String sdkError = createSDKError(bannerErrorInfo);
      Log.w(TAG, "Unity Ads banner failed to load: " + sdkError);

      if (mMediationBannerListener != null) {
        int errorCode = getMediationErrorCode(bannerErrorInfo);
        mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, errorCode);
      }
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
      Log.v(TAG, "Unity Ads banner for placement ID '" + mBannerView.getPlacementId()
          + "' has left the application.");
      mMediationBannerListener.onAdLeftApplication(UnityBannerAd.this);
    }
  };

  @Override
  public void onDestroy() {
    if (mBannerView != null) {
      mBannerView.destroy();
    }
    mBannerView = null;
    mMediationBannerListener = null;
    mUnityBannerListener = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  public void requestBannerAd(final Context context, MediationBannerListener listener,
      Bundle serverParameters,
      final AdSize adSize, MediationAdRequest adRequest, Bundle mediationExtras) {
    mMediationBannerListener = listener;

    gameId = serverParameters.getString(KEY_GAME_ID);
    bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

    if (!UnityAdapter.isValidIds(gameId, bannerPlacementId)) {
      if (mMediationBannerListener == null) {
        return;
      }
      String adapterError = createAdapterError(
          ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      mMediationBannerListener
          .onAdFailedToLoad(UnityBannerAd.this, ERROR_INVALID_SERVER_PARAMETERS);
      return;
    }

    if (context == null || !(context instanceof Activity)) {
      String adapterError = createAdapterError(ERROR_CONTEXT_NOT_ACTIVITY,
          "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener
            .onAdFailedToLoad(UnityBannerAd.this, ERROR_CONTEXT_NOT_ACTIVITY);
      }
      return;
    }

    if (adSize == null) {
      String adapterError = createAdapterError(
          ERROR_BANNER_SIZE_MISMATCH,
          "Unity banner ad failed to load : banner size is invalid.");
      Log.e(TAG, adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener
            .onAdFailedToLoad(UnityBannerAd.this, ERROR_BANNER_SIZE_MISMATCH);
      }
      return;
    }

    final UnityBannerSize unityBannerSize = UnityAdsAdapterUtils
        .getUnityBannerSize(context, adSize);

    if (unityBannerSize == null) {
      String adapterError = createAdapterError(
          ERROR_BANNER_SIZE_MISMATCH,
          "There is no matching UnityAds ad size for Google ad size: " + adSize);
      Log.w(TAG, adapterError);
      if (mMediationBannerListener != null) {
        mMediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, ERROR_BANNER_SIZE_MISMATCH);
      }
      return;
    }

    UnityInitializer.getInstance()
        .initializeUnityAds(context, gameId, new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(TAG, "Unity Ads successfully initialized, can now load " +
                "banner ad for placement ID '" + bannerPlacementId + "' in game '" + gameId + "'.");

            if (mBannerView == null) {
              mBannerView = new BannerView((Activity) context, bannerPlacementId, unityBannerSize);
            }

            mBannerView.setListener(mUnityBannerListener);
            mBannerView.load();
          }

          @Override
          public void onInitializationFailed(UnityAds.UnityAdsInitializationError
              unityAdsInitializationError, String errorMessage) {

            AdError adError = createSDKError(unityAdsInitializationError,
                "Unity Ads initialization failed: [" +
                    unityAdsInitializationError + "] " + errorMessage +
                    ", cannot load banner ad for placement ID '" + bannerPlacementId
                    + "' in game '" + gameId + "'");
            Log.e(TAG, adError.toString());

            if (mMediationBannerListener != null) {
              mMediationBannerListener
                  .onAdFailedToLoad(UnityBannerAd.this, adError);
            }
          }
        });
  }

  @Override
  public View getBannerView() {
    return mBannerView;
  }

}