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

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.WATERMARK;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKInitializationError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKLoadError;
import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.createSDKShowError;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ADAPTER_ERROR_DOMAIN;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_MISSING_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_MSG_NON_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.LoadListener;
import com.unity3d.ads.RewardedAd;
import com.unity3d.ads.RewardedShowListener;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAdsError;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The {@link UnityRewardedAd} is used to load Unity Rewarded ads and mediate the callbacks between
 * Google Mobile Ads SDK and Unity Ads SDK.
 *
 * <p><b>Note:</b> This class is not thread-safe.
 */
public class UnityRewardedAd implements MediationRewardedAd {

  /**
   * The loaded RewardedAd instance from Unity Ads SDK.
   */
  @Nullable
  private RewardedAd loadedRewardedAd;

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

  private final String watermark;

  /** IUnityAdsLoadListener instance. */
  @VisibleForTesting
  final LoadListener<RewardedAd> loadListener =
      new LoadListener<>() {
        @Override
        public void onAdLoaded(@Nullable RewardedAd rewardedAd, @Nullable UnityAdsError loadError) {
          if (loadError == null) {
            // Success
            String logMessage1 =
                    String.format("Unity Ads rewarded ad successfully loaded for " + "placement " +
                            "ID: %s", placementId);
            Log.d(TAG, logMessage1);
            loadedRewardedAd = rewardedAd;
            mediationRewardedAdCallback =
                    mediationAdLoadCallback.onSuccess(UnityRewardedAd.this);
          } else {
            // Failure
            AdError adError = createSDKLoadError(loadError, loadError.getMessage());
            Log.w(TAG, loadError.toString());
            mediationAdLoadCallback.onFailure(adError);
          }
        }
      };

  public UnityRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback,
      @NonNull UnityInitializer unityInitializer,
      @NonNull UnityAdsLoader unityAdsLoader) {
    watermark = mediationRewardedAdConfiguration.getWatermark();
    this.mediationAdLoadCallback = callback;
    this.unityInitializer = unityInitializer;
    this.unityAdsLoader = unityAdsLoader;
  }

  /** Loads a rewarded ad. */
  public void loadAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration) {
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

    if (loadedRewardedAd == null) {
      Log.w(TAG, "Unity Ads received call to show before successfully loading an ad.");
      if (mediationRewardedAdCallback != null) {
        AdError adError = new AdError(UnityMediationAdapter.ERROR_AD_NOT_READY,
                "RewardedAd is " + "not loaded", ADAPTER_ERROR_DOMAIN);
        mediationRewardedAdCallback.onAdFailedToShow(adError);
      }
      return;
    }

    Activity activity = (Activity) context;

    // Build ShowConfiguration
    ShowConfiguration.Builder builder =
            new ShowConfiguration.Builder();

    if (watermark != null) {
      Map<String, String> extras = new HashMap<>();
      extras.put(WATERMARK, watermark);
      builder.withExtras(extras);
    }

    ShowConfiguration showConfig = builder.build();

    // Show the ad
    loadedRewardedAd.show(activity, showConfig, unityShowListener);
  }

  /** RewardedShowListener instance. Contains logic for callbacks when showing ads. */
  @VisibleForTesting
  final RewardedShowListener unityShowListener =
      new RewardedShowListener() {
        @Override
        public void onStarted(RewardedAd rewardedAd) {
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
        public void onClicked(RewardedAd rewardedAd) {
          // Unity Ads ad clicked.
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.reportAdClicked();
          }
        }

        @Override
        public void onCompleted(RewardedAd rewardedAd, @NonNull ShowFinishState state) {
          // Unity Ads ad closed.
          if (mediationRewardedAdCallback == null) {
            return;
          }
          if (state == ShowFinishState.COMPLETED) {
            mediationRewardedAdCallback.onVideoComplete();
          }
          mediationRewardedAdCallback.onAdClosed();
        }

        @Override
        public void onFailed(RewardedAd rewardedAd, @NonNull UnityAdsError unityAdsError) {
          // Unity Ads ad failed to show.
          if (mediationRewardedAdCallback != null) {
            AdError adError = createSDKShowError(unityAdsError, unityAdsError.getMessage());
            mediationRewardedAdCallback.onAdFailedToShow(adError);
          }
        }

        @Override
        public void onRewarded(@NonNull RewardedAd rewardedAd) {
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onUserEarnedReward();
          }
        }
      };

  private class UnityAdsInitializationListener implements InitializationListener {
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
    public void onInitializationComplete(@Nullable UnityAdsError initializationError) {
      if (initializationError == null) {
        String logMessage =
            String.format(
                "Unity Ads is initialized for game ID '%s' "
                    + "and can now load rewarded ad with placement ID: %s",
                gameId, placementId);
        Log.d(TAG, logMessage);

        UnityAdsAdapterUtils.setUnityAdsPrivacy(MobileAds.getRequestConfiguration());

        objectId = UUID.randomUUID().toString();

        // Use new load API
        unityAdsLoader.loadRewarded(placementId, adMarkup, objectId, loadListener);
      } else {
        String adErrorMessage =
                String.format("Unity Ads initialization failed for game ID '%s' " + "with error " +
                        "message: %s", gameId, initializationError.getMessage());
        AdError adError = createSDKInitializationError(initializationError, adErrorMessage);
        Log.w(TAG, adError.toString());
        mediationAdLoadCallback.onFailure(adError);
      }
    }
  }
}
