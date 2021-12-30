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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.unity.eventadapters.UnityBannerEventAdapter;
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
  private BannerView bannerView;

  /**
   * Callback object for Google's Banner Lifecycle.
   */
  private MediationBannerListener mediationBannerListener;

  /**
   * UnityBannerEventAdapter instance to send events from the mediationBannerListener.
   */
  private UnityBannerEventAdapter eventAdapter;

  /**
   * BannerView.IListener instance.
   */
  private BannerView.IListener mUnityBannerListener = new BannerView.Listener() {
    @Override
    public void onBannerLoaded(BannerView bannerView) {
      String logMessage = String.format("Unity Ads finished loading banner ad for placement ID: %s",
          UnityBannerAd.this.bannerView.getPlacementId());
      Log.d(TAG, logMessage);
      eventAdapter.sendAdEvent(AdEvent.LOADED);
    }

    @Override
    public void onBannerClick(BannerView bannerView) {
      String logMessage = String.format("Unity Ads banner ad was clicked for placement ID: %s",
          UnityBannerAd.this.bannerView.getPlacementId());
      Log.d(TAG, logMessage);
      eventAdapter.sendAdEvent(AdEvent.CLICKED);
      eventAdapter.sendAdEvent(AdEvent.OPENED);
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
      sendBannerFailedToLoad(getMediationErrorCode(bannerErrorInfo), bannerErrorInfo.errorMessage);
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {
      String logMessage = String.format("Unity Ads banner ad left application for placement ID: %s",
          UnityBannerAd.this.bannerView.getPlacementId());
      Log.d(TAG, logMessage);
      eventAdapter.sendAdEvent(AdEvent.LEFT_APPLICATION);
    }
  };

  @Override
  public void onDestroy() {
    if (bannerView != null) {
      bannerView.destroy();
    }
    bannerView = null;
    mediationBannerListener = null;
    mUnityBannerListener = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  public void requestBannerAd(@NonNull Context context, @NonNull MediationBannerListener listener,
      @NonNull Bundle serverParameters, @NonNull AdSize adSize,
      @NonNull MediationAdRequest adRequest, @Nullable Bundle mediationExtras) {
    mediationBannerListener = listener;
    eventAdapter = new UnityBannerEventAdapter(mediationBannerListener, this);

    gameId = serverParameters.getString(KEY_GAME_ID);
    bannerPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

    if (!UnityAdapter.areValidIds(gameId, bannerPlacementId)) {
      sendBannerFailedToLoad(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid server parameters.");
      return;
    }

    if (!(context instanceof Activity)) {
      sendBannerFailedToLoad(ERROR_CONTEXT_NOT_ACTIVITY,
          "Unity Ads requires an Activity context to load ads.");
      return;
    }
    Activity activity = (Activity) context;

    UnityBannerSize unityBannerSize = UnityAdsAdapterUtils.getUnityBannerSize(context, adSize);
    if (unityBannerSize == null) {
      String errorMessage = String
          .format("There is no matching Unity Ads ad size for Google ad size: %s",
              adSize);
      sendBannerFailedToLoad(ERROR_BANNER_SIZE_MISMATCH, errorMessage);
      return;
    }

    UnityInitializer.getInstance()
        .initializeUnityAds(context, gameId, new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            String logMessage = String.format("Unity Ads is initialized for game ID '%s' "
                + "and can now load banner ad with placement ID: %s", gameId, bannerPlacementId);
            Log.d(TAG, logMessage);

            if (bannerView == null) {
              bannerView = new BannerView(activity, bannerPlacementId, unityBannerSize);
            }

            bannerView.setListener(mUnityBannerListener);
            bannerView.load();
          }

          @Override
          public void onInitializationFailed(
              UnityAds.UnityAdsInitializationError unityAdsInitializationError,
              String errorMessage) {
            String adErrorMessage = String
                .format("Unity Ads initialization failed for game ID '%s' with error message: %s",
                    gameId, errorMessage);
            AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
            Log.w(TAG, adError.toString());

            if (mediationBannerListener != null) {
              mediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, adError);
            }
          }
        });
  }

  @Override
  public View getBannerView() {
    return bannerView;
  }

  private void sendBannerFailedToLoad(int errorCode, String errorDescription) {
    AdError loadError = createAdError(errorCode, errorDescription);
    Log.w(TAG, loadError.toString());

    if (mediationBannerListener != null) {
      mediationBannerListener.onAdFailedToLoad(UnityBannerAd.this, loadError);
    }
  }

}