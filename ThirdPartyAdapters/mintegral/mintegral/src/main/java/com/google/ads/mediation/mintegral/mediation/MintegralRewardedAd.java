// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;
import com.mbridge.msdk.out.RewardVideoWithCodeListener;

public abstract class MintegralRewardedAd extends RewardVideoWithCodeListener implements
    MediationRewardedAd {

  protected final MediationRewardedAdConfiguration adConfiguration;
  protected final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;
  protected MediationRewardedAdCallback rewardedAdCallback;

  public MintegralRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  /**
   * Loads a Mintegral rewarded ad.
   */
  public abstract void loadAd();

  @Override
  public void onVideoLoadSuccess(MBridgeIds mBridgeIds) {
    rewardedAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onLoadSuccess(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onVideoLoadFailWithCode(MBridgeIds mBridgeIds, int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode, errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onAdShow(MBridgeIds mBridgeIds) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdOpened();
      rewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    if (rewardedAdCallback == null) {
      return;
    }
    if (rewardInfo != null && rewardInfo.isCompleteView()) {
      RewardItem rewardItem = new RewardItem() {
        @NonNull
        @Override
        public String getType() {
          return rewardInfo.getRewardName();
        }

        @Override
        public int getAmount() {
          int amount = 0;
          try {
            amount = Integer.getInteger(rewardInfo.getRewardAmount());
          } catch (Exception exception) {
            Log.w(TAG, "Failed to get reward amount.", exception);
          }
          return amount;
        }
      };
      rewardedAdCallback.onUserEarnedReward(rewardItem);
    } else {
      Log.w(TAG, "Mintegral SDK failed to reward user due to missing rewarded settings "
          + "or rewarded ad playback not completed.");
    }
    rewardedAdCallback.onAdClosed();
  }

  @Override
  public void onShowFailWithCode(MBridgeIds mBridgeIds, int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode, errorMessage);
    Log.w(TAG, error.toString());
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onVideoAdClicked(MBridgeIds mBridgeIds) {
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onVideoComplete(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
