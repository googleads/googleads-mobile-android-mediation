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
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.unity.eventadapters.UnityRewardedEventAdapter;
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

public class UnityRewardedAd implements MediationRewardedAd {

  /**
   * Mediation rewarded video ad listener used to forward ad load status to the Google Mobile Ads
   * SDK.
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Mediation rewarded video ad listener used to forward rewarded ad events to the Google Mobile
   * Ads SDK.
   */
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  /**
   * Placement ID used to determine what type of ad to load.
   */
  private String placementId;

  /**
   * UnityRewardedEventAdapter instance to send events from the mediationRewardedAdCallback.
   */
  private UnityRewardedEventAdapter eventAdapter;

  /**
   * IUnityAdsLoadListener instance.
   */
  private final IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
      String logMessage = String
          .format("Unity Ads rewarded ad successfully loaded placement ID: %s", placementId);
      Log.d(TAG, logMessage);
      UnityRewardedAd.this.placementId = placementId;
      sendRewardedLoadSuccess();
    }

    @Override
    public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error,
        String message) {
      UnityRewardedAd.this.placementId = placementId;
      AdError adError = createSDKError(error, message);
      sendRewardedLoadFailure(adError);
    }
  };

  /**
   * Loads a rewarded ad.
   */
  public void load(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.mediationAdLoadCallback = callback;

    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      sendRewardedLoadFailure(createAdError(ERROR_CONTEXT_NOT_ACTIVITY,
          "Unity Ads requires an Activity context to load ads."));
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    String placementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);
    if (!UnityAdapter.areValidIds(gameId, placementId)) {
      sendRewardedLoadFailure(
          createAdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid server parameters."));
      return;
    }

    UnityInitializer.getInstance().initializeUnityAds(context, gameId,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            String logMessage = String.format("Unity Ads is initialized for game ID '%s' "
                + "and can now load rewarded ad with placement ID: %s", gameId, placementId);
            Log.d(TAG, logMessage);
          }

          @Override
          public void onInitializationFailed(
              UnityAds.UnityAdsInitializationError unityAdsInitializationError,
              String errorMessage) {
            String adErrorMessage = String
                .format("Unity Ads initialization failed for game ID '%s' with error message: %s",
                    gameId, errorMessage);
            AdError adError = createSDKError(unityAdsInitializationError, adErrorMessage);
            sendRewardedLoadFailure(adError);
          }
        });

    UnityAds.load(placementId, mUnityLoadListener);
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (!(context instanceof Activity)) {
      AdError showError = createAdError(ERROR_CONTEXT_NOT_ACTIVITY,
          "Unity Ads requires an Activity context to show ads.");
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

    // UnityAds can handle a null placement ID so show is always called here.
    UnityAds.show(activity, placementId, mUnityShowListener);

    // Unity Ads does not have an ad opened callback.
    eventAdapter.sendAdEvent(AdEvent.OPENED);
  }

  /**
   * IUnityAdsShowListener instance. Contains logic for callbacks when showing ads.
   */
  private final IUnityAdsShowListener mUnityShowListener = new IUnityAdsShowListener() {
    @Override
    public void onUnityAdsShowStart(String placementId) {
      // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
      // video.
      eventAdapter.sendAdEvent(AdEvent.IMPRESSION);
      eventAdapter.sendAdEvent(AdEvent.VIDEO_START);
    }

    @Override
    public void onUnityAdsShowClick(String placementId) {
      // Unity Ads ad clicked.
      eventAdapter.sendAdEvent(AdEvent.CLICKED);
    }

    @Override
    public void onUnityAdsShowComplete(String placementId,
        UnityAds.UnityAdsShowCompletionState state) {
      // Unity Ads ad closed.
      // Reward is provided only if the ad is watched completely.
      if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
        eventAdapter.sendAdEvent(AdEvent.VIDEO_COMPLETE);
        eventAdapter.sendAdEvent(AdEvent.REWARD);
      }
      eventAdapter.sendAdEvent(AdEvent.CLOSED);
    }

    @Override
    public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
      // Unity Ads ad failed to show.
      if (mediationRewardedAdCallback != null) {
        AdError adError = createSDKError(error, message);
        mediationRewardedAdCallback.onAdFailedToShow(adError);
      }
    }
  };

  private void sendRewardedLoadSuccess() {
    if (mediationAdLoadCallback != null) {
      mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(UnityRewardedAd.this);
      eventAdapter = new UnityRewardedEventAdapter(mediationRewardedAdCallback);
    }
  }

  private void sendRewardedLoadFailure(AdError adError) {
    Log.w(TAG, adError.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(adError);
    }
  }
}
