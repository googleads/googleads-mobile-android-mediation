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
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_AD_ALREADY_LOADING;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY;
import static com.google.ads.mediation.unity.UnityMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.unity.UnityMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class UnityRewardedAd implements MediationRewardedAd {

  /**
   * Mediation rewarded video ad listener used to forward ad load status to the Google Mobile Ads
   * SDK.
   */
  private MediationAdLoadCallback<MediationRewardedAd,
      MediationRewardedAdCallback> mMediationAdLoadCallback;

  /**
   * Mediation rewarded video ad listener used to forward rewarded ad events to the Google Mobile
   * Ads SDK.
   */
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  /**
   * Placement ID used to determine what type of ad to load.
   */
  private String mPlacementId;

  /**
   * A list of placement IDs that are currently loaded to prevent duplicate requests.
   */
  private static final HashMap<String, WeakReference<UnityRewardedAd>> mPlacementsInUse = new HashMap<>();

  /**
   * IUnityAdsLoadListener instance.
   */
  private final IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String placementId) {
      Log.d(TAG, "Unity Ads rewarded ad successfully loaded for placement ID '"
          + placementId + "'");
      mPlacementId = placementId;
      if (mMediationAdLoadCallback == null) {
        return;
      }
      mMediationRewardedAdCallback = mMediationAdLoadCallback.onSuccess(UnityRewardedAd.this);
    }

    @Override
    public void onUnityAdsFailedToLoad(String placementId, UnityAdsLoadError error,
        String message) {
      mPlacementId = placementId;
      mPlacementsInUse.remove(mPlacementId);
      AdError adError = createSDKError(error, message);
      Log.w(TAG, adError.toString());
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(adError);
      }
    }
  };

  /**
   * Loads a rewarded ad.
   */
  public void load(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.mMediationAdLoadCallback = callback;

    Context context = mediationRewardedAdConfiguration.getContext();
    if (context == null || !(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to load ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(adapterError);
      }
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    final String gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    final String placementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);

    if (!UnityAdapter.isValidIds(gameId, placementId)) {
      String adapterError =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid server parameters.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(adapterError);
      }
      return;
    }

    UnityInitializer.getInstance().initializeUnityAds(context, gameId,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(TAG, "Unity Ads successfully initialized, can now " +
                "load rewarded ad for placement ID '" + placementId + "' in game " +
                "'" + gameId + "'.");
          }

          @Override
          public void onInitializationFailed(UnityAds.UnityAdsInitializationError
              unityAdsInitializationError, String errorMessage) {
            AdError adError = createSDKError(unityAdsInitializationError, errorMessage);
            Log.w(TAG, adError.toString());
            mMediationAdLoadCallback.onFailure(adError);
          }
        });

    if (mPlacementsInUse.containsKey(placementId)
        && mPlacementsInUse.get(placementId).get() != null) {
      WeakReference<UnityRewardedAd> adapterRef = mPlacementsInUse.get(placementId);
      if (adapterRef != null && adapterRef.get() != null) {
        if (mMediationAdLoadCallback != null) {
          String adapterError = createAdapterError(ERROR_AD_ALREADY_LOADING,
              "Unity Ads has already loaded placement " + mPlacementId);
          mMediationAdLoadCallback.onFailure(adapterError);
        }
        return;
      }
    }
    mPlacementsInUse.put(placementId, new WeakReference<UnityRewardedAd>(UnityRewardedAd.this));
    UnityAds.load(placementId, mUnityLoadListener);
  }

  @Override
  public void showAd(Context context) {

    mPlacementsInUse.remove(mPlacementId);

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "Unity Ads requires an Activity context to show ads.");
      Log.e(TAG, "Failed to load ad: " + adapterError);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(adapterError);
      }
      return;
    }
    Activity activity = (Activity) context;

    // Check if the placement is ready before showing
    if (mPlacementId == null) {
      Log.w(TAG, "Unity Ads received call to show before successfully loading an ad");
    }

    // UnityAds can handle a null placement ID so show is always called here.
    UnityAds.show(activity, mPlacementId, mUnityShowListener);

    // Unity Ads does not have an ad opened callback.
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdOpened();
    }
  }

  /**
   * IUnityAdsShowListener instance. Contains logic for callbacks when showing ads.
   */
  private final IUnityAdsShowListener mUnityShowListener = new IUnityAdsShowListener() {
    @Override
    public void onUnityAdsShowStart(String placementId) {
      // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
      // video.
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.reportAdImpression();
        mMediationRewardedAdCallback.onVideoStart();
      }
    }

    @Override
    public void onUnityAdsShowClick(String placementId) {
      // Unity Ads ad clicked.
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.reportAdClicked();
      }
    }

    @Override
    public void onUnityAdsShowComplete(String placementId,
        UnityAds.UnityAdsShowCompletionState state) {
      // Unity Ads ad closed.
      if (mMediationRewardedAdCallback == null) {
        return;
      }

      // Reward is provided only if the ad is watched completely.
      if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
        mMediationRewardedAdCallback.onVideoComplete();
        // Unity Ads doesn't provide a reward value. The publisher is expected to
        // override the reward in AdMob console.
        mMediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
      }
      mMediationRewardedAdCallback.onAdClosed();
    }

    @Override
    public void onUnityAdsShowFailure(String placementId, UnityAdsShowError error, String message) {
      // Unity Ads ad failed to show.
      AdError adError = createSDKError(error, message);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(adError);
      }
    }
  };
}
