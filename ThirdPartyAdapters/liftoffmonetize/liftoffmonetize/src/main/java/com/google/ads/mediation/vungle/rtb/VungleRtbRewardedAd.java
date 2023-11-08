// Copyright 2019 Google LLC
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

package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_ORIENTATION;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_USER_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_CANNOT_PLAY_AD;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleError;

public class VungleRtbRewardedAd implements MediationRewardedAd, RewardedAdListener {

  @NonNull
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  @NonNull
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  @Nullable
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  private RewardedAd rewardedAd;

  public VungleRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    String userId = mediationExtras.getString(KEY_USER_ID);

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load bidding rewarded ad from Liftoff Monetize. "
                  + "Missing or invalid App ID configured for this ad source instance "
                  + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placement = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placement)) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load bidding rewarded ad from Liftoff Monetize. "
                  + "Missing or invalid Placement ID configured for this ad source instance "
                  + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String adMarkup = mediationRewardedAdConfiguration.getBidResponse();

    AdConfig adConfig = new AdConfig();
    if (mediationExtras.containsKey(KEY_ORIENTATION)) {
      adConfig.setAdOrientation(mediationExtras.getInt(KEY_ORIENTATION, AdConfig.AUTO_ROTATE));
    }
    String watermark = mediationRewardedAdConfiguration.getWatermark();
    if (!TextUtils.isEmpty(watermark)) {
      adConfig.setWatermark(watermark);
    }

    Context context = mediationRewardedAdConfiguration.getContext();

    VungleInitializer.getInstance()
        .initialize(appID, context,
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                rewardedAd = new RewardedAd(context, placement, adConfig);
                rewardedAd.setAdListener(VungleRtbRewardedAd.this);
                if (!TextUtils.isEmpty(userId)) {
                  rewardedAd.setUserId(userId);
                }
                rewardedAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (rewardedAd != null) {
      rewardedAd.play(context);
    } else if (mediationRewardedAdCallback != null) {
      AdError error = new AdError(ERROR_CANNOT_PLAY_AD, "Failed to show bidding rewarded"
          + "ad from Liftoff Monetize.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdLoaded(@NonNull BaseAd baseAd) {
    mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
  }

  @Override
  public void onAdStart(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdEnd(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClicked(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdRewarded(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoComplete();
      mediationRewardedAdCallback.onUserEarnedReward(
          new VungleMediationAdapter.VungleReward("vungle", 1));
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    Log.w(TAG, error.toString());
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    Log.w(TAG, error.toString());
    mediationAdLoadCallback.onFailure(error);
  }

  @Override
  public void onAdImpression(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoStart();
      mediationRewardedAdCallback.reportAdImpression();
    }
  }

}
