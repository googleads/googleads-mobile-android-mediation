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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKError;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.KEY_WATERMARK;
import static com.google.ads.mediation.unity.UnityMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsLoadError;
import com.unity3d.ads.UnityAds.UnityAdsShowError;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import java.util.UUID;

/**
 * The {@link UnityRewardedAd} is used to load Unity Rewarded ads and mediate the callbacks between
 * Google Mobile Ads SDK and Unity Ads SDK.
 *
 * <p><b>Note:</b> This class is not thread-safe.
 */
public class UnityRewardedAd implements MediationRewardedAd {

  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  /**
   * Mediation rewarded video ad listener used to forward ad load status to the Google Mobile Ads
   * SDK.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  private final UnityInitializer unityInitializer;

  private final UnityAdsLoader unityAdsLoader;

  /**
   * Mediation rewarded video ad listener used to forward rewarded ad events to the Google Mobile
   * Ads SDK.
   */
  @Nullable private MediationRewardedAdCallback mediationRewardedAdCallback;

  /** Placement ID used to determine what type of ad to load. */
  @Nullable private String placementId;

  /** Object ID used to track loaded/shown ads. */
  @Nullable private String objectId;

  /** IUnityAdsLoadListener instance. */
  @VisibleForTesting
  final IUnityAdsLoadListener unityLoadListener =
      new IUnityAdsLoadListener() {
        @Override
        public void onUnityAdsAdLoaded(String placementId) {
          String logMessage =
              String.format(
                  "Unity Ads rewarded ad successfully loaded placement ID: %s", placementId);
          Log.d(TAG, logMessage);
          UnityRewardedAd.this.placementId = placementId;
          mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(UnityRewardedAd.this);
        }

        @Override
        public void onUnityAdsFailedToLoad(
            String placementId, UnityAdsLoadError error, String message) {
          UnityRewardedAd.this.placementId = placementId;
          AdError adError = createSDKError(error, message);
          Log.w(TAG, adError.toString());
          mediationAdLoadCallback.onFailure(adError);
        }
      };

  public UnityRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback,
      @NonNull UnityInitializer unityInitializer,
      @NonNull UnityAdsLoader unityAdsLoader) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = callback;
    this.unityInitializer = unityInitializer;
    this.unityAdsLoader = unityAdsLoader;
  }

  /** Loads a rewarded ad. */
  public void loadAd() {

    Context context = mediationRewardedAdConfiguration.getContext();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    final String gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    final String placementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);
    if (!UnityAdsAdapterUtils.areValidIds(gameId, placementId)) {
      AdError adError =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, ERROR_MSG_MISSING_PARAMETERS, ADAPTER_ERROR_DOMAIN);
      Log.w(TAG, adError.toString());
      mediationAdLoadCallback.onFailure(adError);
      return;
    }
    final String adMarkup = mediationRewardedAdConfiguration.getBidResponse();

    // The ad is loaded in the UnityAdsInitializationListener after initializing of the Unity Ads
    // SDK.
    unityInitializer.initializeUnityAds(
        context, gameId, new UnityAdsInitializationListener(context, gameId, placementId, adMarkup));
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (!(context instanceof Activity)) {
      AdError showError =
          new AdError(ERROR_CONTEXT_NOT_ACTIVITY, ERROR_MSG_NON_ACTIVITY, ADAPTER_ERROR_DOMAIN);
      Log.e(TAG, showError.toString());
      if (mediationRewardedAdCallback != null) {
        mediationRewardedAdCallback.onAdFailedToShow(showError);
      }
      return;
    }
    Activity activity = (Activity) context;

    // Check if the placement is ready before showing
    if (placementId == null) {
      Log.w(TAG, "Unity Ads received call to show before successfully loading an ad.");
    }

    UnityAdsShowOptions unityAdsShowOptions =
        unityAdsLoader.createUnityAdsShowOptionsWithId(objectId);
    unityAdsShowOptions.set(KEY_WATERMARK, mediationRewardedAdConfiguration.getWatermark());

    // UnityAds can handle a null placement ID so show is always called here.
    unityAdsLoader.show(activity, placementId, unityAdsShowOptions, unityShowListener);
  }

  /** IUnityAdsShowListener instance. Contains logic for callbacks when showing ads. */
  @VisibleForTesting
  final IUnityAdsShowListener unityShowListener =
      new IUnityAdsShowListener() {
        @Override
        public void onUnityAdsShowStart(String placementId) {
          // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
          // video.
          if (mediationRewardedAdCallback == null) {
            return;
          }
          mediationRewardedAdCallback.onAdOpened();
          mediationRewardedAdCallback.reportAdImpression();
          mediationRewardedAdCallback.onVideoStart();
        }

        @Override
        public void onUnityAdsShowClick(String placementId) {
          // Unity Ads ad clicked.
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.reportAdClicked();
          }
        }

        @Override
        public void onUnityAdsShowComplete(
            String placementId, UnityAds.UnityAdsShowCompletionState state) {
          // Unity Ads ad closed.
          // Reward is provided only if the ad is watched completely.
          if (mediationRewardedAdCallback == null) {
            return;
          }
          if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
            mediationRewardedAdCallback.onVideoComplete();
            mediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
          }
          mediationRewardedAdCallback.onAdClosed();
        }

        @Override
        public void onUnityAdsShowFailure(
            String placementId, UnityAdsShowError error, String message) {
          // Unity Ads ad failed to show.
          if (mediationRewardedAdCallback != null) {
            AdError adError = createSDKError(error, message);
            mediationRewardedAdCallback.onAdFailedToShow(adError);
          }
        }
      };

  private class UnityAdsInitializationListener implements IUnityAdsInitializationListener {

    private final Context context;
    private final String gameId;
    private final String placementId;
    @Nullable private final String adMarkup;

    UnityAdsInitializationListener(
        Context context, String gameId, String placementId, @Nullable String adMarkup) {
      this.context = context;
      this.gameId = gameId;
      this.placementId = placementId;
      this.adMarkup = adMarkup;
    }

    @Override
    public void onInitializationComplete() {
      String logMessage =
          String.format(
              "Unity Ads is initialized for game ID '%s' "
                  + "and can now load rewarded ad with placement ID: %s",
              gameId, placementId);
      Log.d(TAG, logMessage);
      // TODO(b/280861464): Add setCoppa test when loading ad
      UnityAdsAdapterUtils.setCoppa(
          mediationRewardedAdConfiguration.taggedForChildDirectedTreatment(), context);

      objectId = UUID.randomUUID().toString();
      UnityAdsLoadOptions unityAdsLoadOptions =
          unityAdsLoader.createUnityAdsLoadOptionsWithId(objectId);
      if (adMarkup != null) {
        unityAdsLoadOptions.setAdMarkup(adMarkup);
      }

      unityAdsLoader.load(placementId, unityAdsLoadOptions, unityLoadListener);
    }

    @Override
    public void onInitializationFailed(
        UnityAds.UnityAdsInitializationError unityAdsInitializationError, String errorMessage) {
      String adErrorMessage =
          String.format(
              "Unity Ads initialization failed for game ID '%s' with error message: %s",
              gameId, errorMessage);
      AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
      Log.w(TAG, adError.toString());
      mediationAdLoadCallback.onFailure(adError);
    }
  }
}
