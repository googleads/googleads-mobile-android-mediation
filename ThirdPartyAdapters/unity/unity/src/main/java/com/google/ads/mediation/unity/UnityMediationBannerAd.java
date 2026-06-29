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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKInitializationError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKLoadError;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.unity3d.ads.BannerAd;
import com.unity3d.ads.BannerShowListener;
import com.unity3d.ads.BannerSize;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.LoadListener;
import com.unity3d.ads.UnityAdsError;
/**
 * The {@link UnityMediationBannerAd} is used to load Unity Banner ads and mediate the callbacks
 * between Google Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityMediationBannerAd
        implements MediationBannerAd, LoadListener<BannerAd>, BannerShowListener {

  /** The loaded BannerAd instance from Unity Ads SDK. */
  @Nullable private BannerAd loadedBannerAd;

  /** Context used for banner ad */
  @Nullable private Context context;

  /** Placement ID for banner if requested. */
  private String bannerPlacementId;

  /** Game ID, required for loading Unity Ads. */
  private String gameId;

  /** Callback object for Google's Banner Lifecycle. */
  @Nullable private MediationBannerAdCallback mediationBannerAdCallback;

  /** Callback object for Banner Ad Load. */
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationBannerAdLoadCallback;

  private final UnityInitializer unityInitializer;

  private final UnityAdsLoader unityAdsLoader;

  static final String ERROR_MSG_NO_MATCHING_AD_SIZE =
      "There is no matching Unity Ads ad size for Google ad size: ";

  static final String ERROR_MSG_INITIALIZATION_FAILED_FOR_GAME_ID =
      "Unity Ads initialization failed for game ID '%s' with error message: %s";

  public UnityMediationBannerAd(
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              bannerAdLoadCallback,
      @NonNull UnityInitializer unityInitializer,
      @NonNull UnityAdsLoader unityAdsLoader) {
    this.mediationBannerAdLoadCallback = bannerAdLoadCallback;
    this.unityInitializer = unityInitializer;
    this.unityAdsLoader = unityAdsLoader;
  }

  @Override
  public void onAdLoaded(@Nullable BannerAd ad, @Nullable UnityAdsError error) {
    if (error == null) {
      // Success
      String logMessage =
          String.format(
              "Unity Ads finished loading banner ad for placement ID: %s",
          bannerPlacementId);
      Log.d(UnityMediationAdapter.TAG, logMessage);
      loadedBannerAd = ad;
      mediationBannerAdCallback =
              mediationBannerAdLoadCallback.onSuccess(UnityMediationBannerAd.this);
    } else {
      // Failure
      AdError loadError = createSDKLoadError(error, error.getMessage());
      Log.w(UnityMediationAdapter.TAG, loadError.toString());
      mediationBannerAdLoadCallback.onFailure(loadError);
    }
  }

  @Override
  public void onClicked(@NonNull BannerAd bannerAd) {
    String logMessage =
        String.format(
            "Unity Ads banner ad was clicked for placement ID: %s", bannerPlacementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (mediationBannerAdCallback == null) {
      return;
    }

    mediationBannerAdCallback.reportAdClicked();
    mediationBannerAdCallback.onAdOpened();
  }

  @Override
  public void onImpression(@NonNull BannerAd bannerAd) {
    String logMessage =
        String.format(
            "Unity Ads banner ad was shown for placement ID: %s", bannerPlacementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onFailedToShow(@NonNull BannerAd bannerAd, @NonNull UnityAdsError unityAdsError) {
    String logMessage =
        String.format(
            "Unity Ads banner ad failed to show for placement ID: %s", bannerPlacementId);
    Log.d(UnityMediationAdapter.TAG, logMessage);

    // no corresponding callback
  }

  public void loadAd(
      MediationBannerAdConfiguration mediationBannerAdConfiguration,
      MediationUtilsWrapper mediationUtils) {
    Context context = mediationBannerAdConfiguration.getContext();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    bannerPlacementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);

    if (!UnityAdsAdapterUtils.areValidIds(gameId, bannerPlacementId)) {
      AdError adError =
          new AdError(
              UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
              ERROR_MSG_MISSING_PARAMETERS,
              ADAPTER_ERROR_DOMAIN);
      Log.w(UnityMediationAdapter.TAG, adError.toString());
      mediationBannerAdLoadCallback.onFailure(adError);
      return;
    }

    final String adMarkup = mediationBannerAdConfiguration.getBidResponse();
    this.context = context;

    // It is RTB if adMarkup is not empty.
    boolean isRtb = !TextUtils.isEmpty(adMarkup);

    final BannerSize unityBannerSize =
        UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, isRtb, mediationUtils);
    if (unityBannerSize == null) {
      String errorMessage = ERROR_MSG_NO_MATCHING_AD_SIZE + adSize;
      AdError adError =
          new AdError(
              UnityMediationAdapter.ERROR_BANNER_SIZE_MISMATCH, errorMessage, ADAPTER_ERROR_DOMAIN);
      Log.w(UnityMediationAdapter.TAG, adError.toString());
      mediationBannerAdLoadCallback.onFailure(adError);
      return;
    }

    unityInitializer.initializeUnityAds(
        gameId,
        new InitializationListener() {
          @Override
          public void onInitializationComplete(@Nullable UnityAdsError error) {
            if(error == null) {
              // Init success
              String logMessage =
                  String.format(
                      "Unity Ads is initialized for game ID '%s' "
                          + "and can now load banner ad with placement ID: %s",
                      gameId, bannerPlacementId);
              Log.d(UnityMediationAdapter.TAG, logMessage);

              UnityAdsAdapterUtils.setUnityAdsPrivacy(MobileAds.getRequestConfiguration());

              // Use new load API
              unityAdsLoader.loadBanner(
                  bannerPlacementId,
                  unityBannerSize,
                  adMarkup,
                  UnityMediationBannerAd.this,
                  UnityMediationBannerAd.this);

              return;
            }

            // Init fail
            String adErrorMessage =
                String.format(ERROR_MSG_INITIALIZATION_FAILED_FOR_GAME_ID, gameId, error.getMessage());
            AdError adError = createSDKInitializationError(error, adErrorMessage);
            Log.w(UnityMediationAdapter.TAG, adError.toString());

            mediationBannerAdLoadCallback.onFailure(adError);
          }
        });
  }

  @NonNull
  @Override
  public View getView() {
    if (loadedBannerAd == null) {
      return new View(context);
    }
    return loadedBannerAd.getView();
  }
}
