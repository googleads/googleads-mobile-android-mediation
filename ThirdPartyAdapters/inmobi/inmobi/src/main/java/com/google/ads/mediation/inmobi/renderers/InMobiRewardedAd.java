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

package com.google.ads.mediation.inmobi.renderers;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_DISPLAY_FAILED;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiConstants;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import java.util.Map;

public abstract class InMobiRewardedAd extends InterstitialAdEventListener
    implements MediationRewardedAd {

  private InMobiInterstitial inMobiRewardedAd;

  protected final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;
  protected final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;

  public InMobiRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Invokes the third-party method for loading the ad. */
  protected abstract void internalLoadAd(InMobiInterstitial inMobiRewardedAd);

  public void loadAd() {
    final Context context = mediationRewardedAdConfiguration.getContext();
    final Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    final String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
    final long placementId = InMobiAdapterUtils.getPlacementId(serverParameters);
    AdError error = InMobiAdapterUtils.validateInMobiAdLoadParams(accountID, placementId);
    if (error != null) {
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        createAndLoadRewardAd(context, placementId, mediationAdLoadCallback);
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        Log.w(TAG, error.toString());
        if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(error);
        }
      }
    });
  }

  // region MediationRewardedAd implementation.
  @Override
  public void showAd(Context context) {
    if (!inMobiRewardedAd.isReady()) {
      AdError error = InMobiConstants.createAdapterError(ERROR_AD_NOT_READY,
          "InMobi rewarded ad is not yet ready to be shown.");
      Log.w(TAG, error.toString());

      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    inMobiRewardedAd.show();
  }
  // endregion

  // region Rewarded adapter utility classes.
  private void createAndLoadRewardAd(Context context, long placementId,
      final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    inMobiRewardedAd = new InMobiInterstitial(context, placementId, InMobiRewardedAd.this);

    // Set the COPPA value in InMobi SDK.
    InMobiAdapterUtils.setIsAgeRestricted(mediationRewardedAdConfiguration);

    InMobiAdapterUtils.configureGlobalTargeting(
        mediationRewardedAdConfiguration.getMediationExtras());
    internalLoadAd(inMobiRewardedAd);
  }
  // endregion

  @Override
  public void onAdWillDisplay(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi rewarded ad will be shown.");
  }

  @Override
  public void onAdDisplayed(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi rewarded ad has been shown.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdOpened();
      rewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
    AdError error = InMobiConstants.createAdapterError(ERROR_AD_DISPLAY_FAILED,
        "InMobi rewarded ad failed to show.");
    Log.w(TAG, error.toString());
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi rewarded ad has been dismissed.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi rewarded ad left application.");
  }

  @Override
  public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
      Map<Object, Object> rewards) {
    String rewardKey = "";
    String rewardStringValue = "";
    int rewardValue = 0;

    if (rewards != null) {
      for (Object reward : rewards.keySet()) {
        rewardKey = reward.toString();
        rewardStringValue = rewards.get(rewardKey).toString();
        if (!TextUtils.isEmpty(rewardKey) &&
            !TextUtils.isEmpty(rewardStringValue)) {
          break;
        }
      }
    }

    if (!TextUtils.isEmpty(rewardStringValue)) {
      try {
        rewardValue = Integer.parseInt(rewardStringValue);
      } catch (NumberFormatException e) {
        Log.e(TAG, "Expected an integer reward value. Got " +
            rewardStringValue + " instead. Using reward value of 1.");
        rewardValue = 1;
      }
    }

    if (rewardedAdCallback != null) {
      Log.d(TAG, "InMobi rewarded ad credited the user with a reward.");
      rewardedAdCallback.onVideoComplete();
      rewardedAdCallback.onUserEarnedReward(new InMobiReward(rewardKey, rewardValue));
    }
  }

  @Override
  public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
      Map<Object, Object> parameters) {
    Log.d(TAG, "InMobi rewarded ad has been clicked.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdImpression(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi rewarded ad has logged an impression.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onRequestPayloadCreated(byte[] bytes) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onRequestPayloadCreationFailed(@NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdFetchSuccessful(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi SDK fetched the rewarded ad successfully, " +
        "but the ad contents still need to be loaded.");
  }

  @Override
  public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
    AdError error = InMobiConstants.createSdkError(
        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
        inMobiAdRequestStatus.getMessage());
    Log.w(TAG, error.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdLoadSucceeded(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi rewarded ad has been loaded.");
    if (mediationAdLoadCallback != null) {
      rewardedAdCallback =
          mediationAdLoadCallback.onSuccess(InMobiRewardedAd.this);
    }
  }
}

class InMobiReward implements RewardItem {

  private final String type;
  private final int amount;

  InMobiReward(String type, int amount) {
    this.type = type;
    this.amount = amount;
  }

  @NonNull
  @Override
  public String getType() {
    return type;
  }

  @Override
  public int getAmount() {
    return amount;
  }
}
