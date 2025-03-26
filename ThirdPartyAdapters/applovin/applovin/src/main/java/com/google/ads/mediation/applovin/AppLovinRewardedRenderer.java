// Copyright 2022 Google LLC
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

package com.google.ads.mediation.applovin;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.util.Map;

public abstract class AppLovinRewardedRenderer
    implements MediationRewardedAd, AppLovinAdLoadListener, AppLovinAdRewardListener,
    AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

  protected static final String TAG = AppLovinRewardedRenderer.class.getSimpleName();

  @VisibleForTesting
  protected static final String ERROR_MSG_MULTIPLE_REWARDED_AD =
      "Cannot load multiple rewarded ads with the same Zone ID. Display one ad before attempting to"
          + " load another.";

  @VisibleForTesting protected static final String ERROR_MSG_AD_NOT_READY = "Ad not ready to show.";

  /** Rewarded ad request configuration. */
  protected final MediationRewardedAdConfiguration adConfiguration;

  /** Mediation callback for ad load events. */
  protected final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;

  protected final AppLovinInitializer appLovinInitializer;

  protected final AppLovinAdFactory appLovinAdFactory;

  protected final AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper;

  /**
   * Mediation callback for rewarded ad events.
   */
  @Nullable
  protected MediationRewardedAdCallback rewardedAdCallback;

  /**
   * AppLovin SDK instance.
   */
  @Nullable
  protected AppLovinSdk appLovinSdk;

  /**
   * AppLovin rewarded ad object.
   */
  @Nullable
  protected AppLovinIncentivizedInterstitial incentivizedInterstitial;

  protected AppLovinRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory,
      @NonNull AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
    this.appLovinInitializer = appLovinInitializer;
    this.appLovinAdFactory = appLovinAdFactory;
    this.appLovinSdkUtilsWrapper = appLovinSdkUtilsWrapper;
  }

  /**
   * Loads an AppLovin rewarded ad.
   */
  public abstract void loadAd();

  // region AppLovinAdLoadListener implementation.
  @Override
  public void adReceived(final @NonNull AppLovinAd appLovinAd) {
    Log.i(TAG, "Rewarded video did load ad.");
    appLovinSdkUtilsWrapper.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            rewardedAdCallback = adLoadCallback.onSuccess(AppLovinRewardedRenderer.this);
          }
        });
  }

  @Override
  public void failedToReceiveAd(final int code) {
    AdError error = AppLovinUtils.getAdError(code);
    Log.w(TAG, error.toString());
    appLovinSdkUtilsWrapper.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            adLoadCallback.onFailure(error);
          }
        });
  }
  // endregion

  // region AppLovinAdDisplayListener implementation.
  @Override
  public void adDisplayed(@NonNull AppLovinAd ad) {
    Log.d(TAG, "Rewarded video displayed.");
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.onAdOpened();
    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void adHidden(@NonNull AppLovinAd ad) {
    Log.d(TAG, "Rewarded video dismissed.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
  }
  // endregion

  // region AppLovinAdClickListener implementation.
  @Override
  public void adClicked(@NonNull AppLovinAd ad) {
    Log.d(TAG, "Rewarded video clicked.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }
  // endregion

  // region AppLovinAdVideoPlaybackListener implementation.
  @Override
  public void videoPlaybackBegan(AppLovinAd ad) {
    Log.d(TAG, "Rewarded video playback began.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
    Log.d(TAG, "Rewarded video playback ended at playback percent: " + percentViewed + "%.");
    if (rewardedAdCallback != null && fullyWatched) {
      rewardedAdCallback.onUserEarnedReward();
      rewardedAdCallback.onVideoComplete();
    }
  }
  // endregion

  // region AppLovinAdRewardListener implementation.
  @Override
  public void userOverQuota(AppLovinAd ad, Map<String, String> response) {
    Log.e(
        TAG,
        "Rewarded video validation request for ad did exceed quota with response: " + response);
  }

  @Override
  public void validationRequestFailed(AppLovinAd ad, int code) {
    Log.e(TAG, "Rewarded video validation request for ad failed with error code: " + code);
  }

  @Override
  public void userRewardRejected(AppLovinAd ad, Map<String, String> response) {
    Log.e(TAG, "Rewarded video validation request was rejected with response: " + response);
  }

  @Override
  public void userRewardVerified(AppLovinAd ad, Map<String, String> response) {
    final String currency = response.get("currency");
    final String amountStr = response.get("amount");

    // AppLovin returns amount as double.
    final int amount = (int) Double.parseDouble(amountStr);

    Log.d(TAG, "Rewarded " + amount + " " + currency);
  }
  // endregion
}
