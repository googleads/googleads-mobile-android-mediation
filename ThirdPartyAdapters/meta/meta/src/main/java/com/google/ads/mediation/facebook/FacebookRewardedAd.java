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

package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FAILED_TO_PRESENT_AD;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getPlacementID;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.facebook.ads.Ad;
import com.facebook.ads.AdExperienceType;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookRewardedAd implements MediationRewardedAd, RewardedVideoAdExtendedListener {

  private final MediationRewardedAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Meta Audience Network rewarded video ad instance.
   */
  private RewardedVideoAd rewardedAd;

  /**
   * Flag to determine whether the rewarded ad has been presented.
   */
  private final AtomicBoolean showAdCalled = new AtomicBoolean();

  /**
   * Mediation rewarded video ad listener used to forward rewarded video ad events from the Meta
   * Audience Network SDK to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback rewardedAdCallback;

  private final AtomicBoolean didRewardedAdClose = new AtomicBoolean();

  public FacebookRewardedAd(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.mediationAdLoadCallback = callback;
  }

  public void render() {
    final Context context = adConfiguration.getContext();
    Bundle serverParameters = adConfiguration.getServerParameters();
    final String placementID = getPlacementID(serverParameters);

    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    setMixedAudience(adConfiguration);

    rewardedAd = new RewardedVideoAd(context, placementID);
    if (!TextUtils.isEmpty(adConfiguration.getWatermark())) {
      rewardedAd.setExtraHints(new ExtraHints.Builder()
              .mediationData(adConfiguration.getWatermark()).build());
    }
    rewardedAd.loadAd(
            rewardedAd.buildLoadAdConfig()
                    .withAdListener(this)
                    .withBid(adConfiguration.getBidResponse())
                    .withAdExperience(getAdExperienceType())
                    .build()
    );
  }

  @Override
  public void showAd(@NonNull Context context) {
    showAdCalled.set(true);
    if (!rewardedAd.show()) {
      AdError error = new AdError(ERROR_FAILED_TO_PRESENT_AD, "Failed to present rewarded ad.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
      rewardedAd.destroy();
      return;
    }

    if (rewardedAdCallback != null) {
      rewardedAdCallback.onVideoStart();
      rewardedAdCallback.onAdOpened();
    }
  }

  @NonNull
  AdExperienceType getAdExperienceType() {
    return AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED;
  }

  @Override
  public void onRewardedVideoCompleted() {
    rewardedAdCallback.onVideoComplete();
    rewardedAdCallback.onUserEarnedReward(new FacebookReward());
  }

  @Override
  public void onError(Ad ad, com.facebook.ads.AdError adError) {
    AdError error = FacebookMediationAdapter.getAdError(adError);

    if (showAdCalled.get()) {
      Log.w(TAG, error.getMessage());
      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
    } else {
      Log.w(TAG, error.getMessage());
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
      }
    }

    rewardedAd.destroy();
  }

  @Override
  public void onAdLoaded(Ad ad) {
    if (mediationAdLoadCallback != null) {
      rewardedAdCallback = mediationAdLoadCallback.onSuccess(this);
    }
  }

  @Override
  public void onAdClicked(Ad ad) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onRewardedVideoClosed() {
    if (!didRewardedAdClose.getAndSet(true) && rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
    if (rewardedAd != null) {
      rewardedAd.destroy();
    }
  }

  @Override
  public void onRewardedVideoActivityDestroyed() {
    if (!didRewardedAdClose.getAndSet(true) && rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
    if (rewardedAd != null) {
      rewardedAd.destroy();
    }
  }
}
