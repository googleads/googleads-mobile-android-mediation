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

package com.google.ads.mediation.pangle.renderer;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import com.google.ads.mediation.pangle.PanglePrivacyConfig;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleInitializer;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class PangleRewardedAd implements MediationRewardedAd {

  private final MediationRewardedAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;
  private final PanglePrivacyConfig panglePrivacyConfig;
  private MediationRewardedAdCallback rewardedAdCallback;
  private PAGRewardedAd pagRewardedAd;

  public PangleRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback, PanglePrivacyConfig panglePrivacyConfig) {
    adConfiguration = mediationRewardedAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
    this.panglePrivacyConfig = panglePrivacyConfig;
  }

  public void render() {
    panglePrivacyConfig.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementId = serverParameters.getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load rewarded ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    Context context = adConfiguration.getContext();
    String appId = serverParameters.getString(PangleConstants.APP_ID);
    PangleInitializer.getInstance()
        .initialize(
            context,
            appId,
            new Listener() {
              @Override
              public void onInitializeSuccess() {
                PAGRewardedRequest request = new PAGRewardedRequest();
                request.setAdString(bidResponse);
                PAGRewardedAd.loadAd(
                    placementId,
                    request,
                    new PAGRewardedAdLoadListener() {
                      @Override
                      public void onError(int errorCode, String errorMessage) {
                        AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
                        Log.w(TAG, error.toString());
                        adLoadCallback.onFailure(error);
                      }

                      @Override
                      public void onAdLoaded(PAGRewardedAd rewardedAd) {
                        rewardedAdCallback = adLoadCallback.onSuccess(PangleRewardedAd.this);
                        pagRewardedAd = rewardedAd;
                      }
                    });
              }

              @Override
              public void onInitializeError(@NonNull AdError error) {
                Log.w(TAG, error.toString());
                adLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    pagRewardedAd.setAdInteractionListener(
        new PAGRewardedAdInteractionListener() {
          @Override
          public void onAdShowed() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdOpened();
              rewardedAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdClicked() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdDismissed() {
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onAdClosed();
            }
          }

          @Override
          public void onUserEarnedReward(final PAGRewardItem pagRewardItem) {
            RewardItem rewardItem =
                new RewardItem() {
                  @NonNull
                  @Override
                  public String getType() {
                    return pagRewardItem.getRewardName();
                  }

                  @Override
                  public int getAmount() {
                    return pagRewardItem.getRewardAmount();
                  }
                };
            if (rewardedAdCallback != null) {
              rewardedAdCallback.onUserEarnedReward(rewardItem);
            }
          }

          @Override
          public void onUserEarnedRewardFail(int errorCode, String errorMessage) {
            String rewardErrorMessage = String.format("Failed to reward user: %s", errorMessage);
            AdError error = PangleConstants.createSdkError(errorCode, rewardErrorMessage);
            Log.d(TAG, error.toString());
          }
        });

    if (context instanceof Activity) {
      pagRewardedAd.show((Activity) context);
      return;
    }
    // If the context is not an Activity, the application context will be used to render the ad.
    pagRewardedAd.show(null);
  }
}
