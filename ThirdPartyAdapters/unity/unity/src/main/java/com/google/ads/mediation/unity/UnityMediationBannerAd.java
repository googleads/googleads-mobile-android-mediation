// Copyright 2020 Google LLC
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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createAdError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationErrorCode;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

/**
 * The {@link UnityMediationBannerAd} is used to load Unity Banner ads and mediate the callbacks
 * between Google Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityMediationBannerAd implements MediationBannerAd, BannerView.IListener {

  /** Placement ID for banner if requested. */
  private String bannerPlacementId;

  /** Game ID, required for loading Unity Ads. */
  private String gameId;

  /** The view for the banner instance. */
  private BannerView bannerView;

  /** Callback object for Google's Banner Lifecycle. */
  @Nullable private MediationBannerAdCallback mediationBannerAdCallback;

  /** Callback object for Banner Ad Load. */
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationBannerAdLoadCallback;

  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;

  static final String ERROR_MSG_NO_MATCHING_AD_SIZE =
      "There is no matching Unity Ads ad size for Google ad size: ";

  public UnityMediationBannerAd(
      @NonNull MediationBannerAdConfiguration bannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              bannerAdLoadCallback) {
    this.mediationBannerAdConfiguration = bannerAdConfiguration;
    this.mediationBannerAdLoadCallback = bannerAdLoadCallback;
  }

  @Override
  public void onBannerLoaded(BannerView bannerView) {
    String logMessage =
        String.format(
            "Unity Ads finished loading banner ad for placement ID: %s",
            bannerView.getPlacementId());
    Log.d(UnityMediationAdapter.TAG, logMessage);
    mediationBannerAdCallback = mediationBannerAdLoadCallback.onSuccess(this);
    // TODO(b/276467762): Find a place to call mediatinoBannerAdCallback.reportAdImpression(), if
    // any.
  }

  @Override
  public void onBannerClick(BannerView bannerView) {
    String logMessage =
        String.format(
            "Unity Ads banner ad was clicked for placement ID: %s", bannerView.getPlacementId());
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (mediationBannerAdCallback == null) {
      return;
    }

    mediationBannerAdCallback.reportAdClicked();
    mediationBannerAdCallback.onAdOpened();
  }

  @Override
  public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
    int errorCode = getMediationErrorCode(bannerErrorInfo);
    AdError loadError = createAdError(errorCode, bannerErrorInfo.errorMessage);
    Log.w(UnityMediationAdapter.TAG, loadError.toString());
    mediationBannerAdLoadCallback.onFailure(loadError);
  }

  @Override
  public void onBannerLeftApplication(BannerView bannerView) {
    String logMessage =
        String.format(
            "Unity Ads banner ad left application for placement ID: %s",
            bannerView.getPlacementId());
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (mediationBannerAdCallback == null) {
      return;
    }

    mediationBannerAdCallback.onAdLeftApplication();
  }

  public void loadAd() {
    Context context = mediationBannerAdConfiguration.getContext();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    bannerPlacementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);

    if (!UnityAdapter.areValidIds(gameId, bannerPlacementId)) {
      AdError adError =
          new AdError(
              UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
              ERROR_MSG_MISSING_PARAMETERS,
              ADAPTER_ERROR_DOMAIN);
      Log.w(UnityMediationAdapter.TAG, adError.toString());
      mediationBannerAdLoadCallback.onFailure(adError);
      return;
    }

    if (!(context instanceof Activity)) {
      AdError adError =
          new AdError(
              UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY,
              ERROR_MSG_NON_ACTIVITY,
              ADAPTER_ERROR_DOMAIN);
      Log.w(UnityMediationAdapter.TAG, adError.toString());
      mediationBannerAdLoadCallback.onFailure(adError);
      return;
    }
    final Activity activity = (Activity) context;

    final UnityBannerSize unityBannerSize =
        UnityAdsAdapterUtils.getUnityBannerSize(context, adSize);
    if (unityBannerSize == null) {
      String errorMessage = ERROR_MSG_NO_MATCHING_AD_SIZE + adSize;
      AdError adError =
          new AdError(
              UnityMediationAdapter.ERROR_BANNER_SIZE_MISMATCH, errorMessage, ADAPTER_ERROR_DOMAIN);
      Log.w(UnityMediationAdapter.TAG, adError.toString());
      mediationBannerAdLoadCallback.onFailure(adError);
      return;
    }

    UnityInitializer.getInstance()
        .initializeUnityAds(
            context,
            gameId,
            new IUnityAdsInitializationListener() {
              @Override
              public void onInitializationComplete() {
                String logMessage =
                    String.format(
                        "Unity Ads is initialized for game ID '%s' "
                            + "and can now load banner ad with placement ID: %s",
                        gameId, bannerPlacementId);
                Log.d(UnityMediationAdapter.TAG, logMessage);

                if (bannerView == null) {
                  bannerView = new BannerView(activity, bannerPlacementId, unityBannerSize);
                }

                UnityAdsAdapterUtils.setCoppa(
                    MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment(), context);

                bannerView.setListener(UnityMediationBannerAd.this);
                bannerView.load();
              }

              @Override
              public void onInitializationFailed(
                  UnityAds.UnityAdsInitializationError unityAdsInitializationError,
                  String errorMessage) {
                String adErrorMessage =
                    String.format(
                        "Unity Ads initialization failed for game ID '%s' with error message: %s",
                        gameId, errorMessage);
                AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
                Log.w(UnityMediationAdapter.TAG, adError.toString());

                mediationBannerAdLoadCallback.onFailure(adError);
              }
            });
  }

  @NonNull
  @Override
  public View getView() {
    return bannerView;
  }
}
