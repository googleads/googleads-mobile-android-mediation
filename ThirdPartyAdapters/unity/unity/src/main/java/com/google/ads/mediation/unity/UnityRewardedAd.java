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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

public class UnityRewardedAd implements MediationRewardedAd, IUnityAdsExtendedListener {

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
   * IUnityAdsLoadListener instance.
   */
  private IUnityAdsLoadListener mUnityLoadListener = new IUnityAdsLoadListener() {
    @Override
    public void onUnityAdsAdLoaded(String s) {
      Log.d(UnityAdapter.TAG, "Unity Ads rewarded ad successfully loaded for placement ID '"
          + s + "'");
      if (mMediationAdLoadCallback != null) {
        mMediationRewardedAdCallback = mMediationAdLoadCallback.onSuccess(UnityRewardedAd.this);
      }

    }

    @Override
    public void onUnityAdsFailedToLoad(String s) {
      Log.e(UnityAdapter.TAG, "Unity Ads rewarded ad load failure for placement ID '" +
          s + "'");
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(s);
      }
    }
  };

  /**
   * Returns the placement ID of the ad being loaded.
   *
   * @return mPlacementId.
   */
  private String getPlacementId() {
    return mPlacementId;
  }

  /**
   * Loads a rewarded ad.
   */
  public void load(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.mMediationAdLoadCallback = callback;

    Context context = mediationRewardedAdConfiguration.getContext();
    if (context == null || !(context instanceof Activity)) {
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure("Context is null or is not an Activity." +
            " Unity Ads requires an Activity context to show ads.");
      }
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    final String gameId = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
    mPlacementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);

    if (!UnityAdapter.isValidIds(gameId, getPlacementId())) {
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure("Failed to load rewarded ad from Unity Ads: " +
            "Missing or invalid game ID and placement ID.");
      }
      return;
    }

    UnityInitializer.getInstance().initializeUnityAds(context, gameId,
        new IUnityAdsInitializationListener() {
          @Override
          public void onInitializationComplete() {
            Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized, can now " +
                "load rewarded ad for placement ID '" + getPlacementId() + "' in game " +
                "'" + gameId + "'.");
            loadRewardedAd(getPlacementId());
          }

          @Override
          public void onInitializationFailed(UnityAds.UnityAdsInitializationError
              unityAdsInitializationError, String s) {
            Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" +
                unityAdsInitializationError + "] " + s + ", cannot load rewarded ad "
                + "for placement ID '" + getPlacementId() + "' in game '" + gameId + "'");
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure("Failed to load rewarded ad from Unity Ads.");
            }
          }
        });
  }

  /**
   * This method will load Unity Ads for a given Placement ID and send the ad loaded event if the
   * ads have already loaded.
   *
   * @param placementId Placement ID of the ad to be loaded
   */
  protected void loadRewardedAd(String placementId) {

    UnityAds.load(placementId, mUnityLoadListener);

  }

  @Override
  public void showAd(Context context) {

    if (!(context instanceof Activity)) {
      String message = "Unity Ads failed to show rewarded ad: activity context is required.";
      Log.w(UnityAdapter.TAG, message);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(message);
      }
      return;
    }
    Activity activity = (Activity) context;

    // Check if the placement is ready before showing
    if (UnityAds.isReady(getPlacementId())) {

      // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
      // Unity Ads fails to show an ad).

      UnityAds.show(activity, getPlacementId());

      // Unity Ads does not have an ad opened callback.
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdOpened();
        mMediationRewardedAdCallback.reportAdImpression();
      }
    } else {
      String message = "Unity Ads failed to show rewarded ad. Placement ID '" +
          getPlacementId() + "' is not ready.";
      Log.w(UnityAdapter.TAG, message);
      if (mMediationRewardedAdCallback != null) {
        mMediationRewardedAdCallback.onAdFailedToShow(message);
      }
    }

  }

  @Override
  public void onUnityAdsReady(String placementId) {
    // Unity Ads is ready to show ads for the given placementId.
  }

  @Override
  public void onUnityAdsStart(String placementId) {
    // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
    // video.
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void onUnityAdsClick(String s) {
    // Unity Ads ad clicked.
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onUnityAdsPlacementStateChanged(String placementId,
      UnityAds.PlacementState oldState,
      UnityAds.PlacementState newState) {
    // This callback is not forwarded to Google Mobile Ads SDK. onUnityAdsError should be used
    // to forward Unity Ads SDK state to Google Mobile Ads SDK.
  }

  @Override
  public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
    // Unity Ads ad closed.
    if (mMediationRewardedAdCallback != null) {
      // Reward is provided only if the ad is watched completely.
      if (finishState == UnityAds.FinishState.COMPLETED) {
        mMediationRewardedAdCallback.onVideoComplete();
        // Unity Ads doesn't provide a reward value. The publisher is expected to
        // override the reward in AdMob console.
        mMediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
      }
      mMediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String errorMessage) {
    // Unity Ads ad failed to show.
    String logMessage = "Failed to show Rewarded ad for placement ID '" + getPlacementId() +
        "' from Unity Ads. Error: " + unityAdsError.toString() + " - " + errorMessage;
    Log.w(UnityAdapter.TAG, logMessage);
    if (mMediationRewardedAdCallback != null) {
      mMediationRewardedAdCallback.onAdFailedToShow(logMessage);
    }
  }
}
