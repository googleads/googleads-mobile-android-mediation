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

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static com.applovin.mediation.ApplovinAdapter.log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.util.Map;

public abstract class AppLovinRewardedRenderer
    implements MediationRewardedAd, AppLovinAdLoadListener, AppLovinAdRewardListener,
    AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

  /**
   * Rewarded ad request configuration.
   */
  @NonNull
  protected MediationRewardedAdConfiguration adConfiguration;

  /**
   * Mediation callback for ad load events.
   */
  @NonNull
  protected MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;

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

  /**
   * Flag to indicate if AppLovin incentivized ad was fully watched.
   */
  private boolean fullyWatched;

  /**
   * AppLovin reward item object.
   */
  private AppLovinRewardItem rewardItem;

  protected AppLovinRewardedRenderer(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
  }

  /**
   * Loads an AppLovin rewarded ad.
   */
  public abstract void loadAd();

  // region AppLovinAdLoadListener implementation.
  @Override
  public void adReceived(final @NonNull AppLovinAd appLovinAd) {
    log(INFO, "Rewarded video did load ad: " + appLovinAd.getAdIdNumber());
    AppLovinSdkUtils.runOnUiThread(
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
    log(WARN, error.toString());

    AppLovinSdkUtils.runOnUiThread(
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
    log(DEBUG, "Rewarded video displayed.");
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.onAdOpened();
    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void adHidden(@NonNull AppLovinAd ad) {
    log(DEBUG, "Rewarded video dismissed.");
    if (rewardedAdCallback == null) {
      return;
    }

    if (fullyWatched) {
      rewardedAdCallback.onUserEarnedReward(rewardItem);
    }
    rewardedAdCallback.onAdClosed();
  }
  // endregion

  // region AppLovinAdClickListener implementation.
  @Override
  public void adClicked(@NonNull AppLovinAd ad) {
    log(DEBUG, "Rewarded video clicked.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }
  // endregion

  // region AppLovinAdVideoPlaybackListener implementation.
  @Override
  public void videoPlaybackBegan(AppLovinAd ad) {
    log(DEBUG, "Rewarded video playback began.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
    log(DEBUG, "Rewarded video playback ended at playback percent: " + percentViewed + "%.");
    this.fullyWatched = fullyWatched;
    if (fullyWatched) {
      rewardedAdCallback.onVideoComplete();
    }
  }
  // endregion

  // region AppLovinAdRewardListener implementation.
  @Override
  public void userOverQuota(AppLovinAd ad, Map<String, String> response) {
    log(ERROR,
        "Rewarded video validation request for ad did exceed quota with response: " + response);
  }

  @Override
  public void validationRequestFailed(AppLovinAd ad, int code) {
    log(ERROR, "Rewarded video validation request for ad failed with error code: " + code);
  }

  @Override
  public void userRewardRejected(AppLovinAd ad, Map<String, String> response) {
    log(ERROR, "Rewarded video validation request was rejected with response: " + response);
  }

  @Override
  public void userRewardVerified(AppLovinAd ad, Map<String, String> response) {
    final String currency = response.get("currency");
    final String amountStr = response.get("amount");

    // AppLovin returns amount as double.
    final int amount = (int) Double.parseDouble(amountStr);

    log(DEBUG, "Rewarded " + amount + " " + currency);
    rewardItem = new AppLovinRewardItem(amount, currency);
  }
  // endregion
}
