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
import static com.google.ads.mediation.inmobi.InMobiConstants.WATERMARK_ALPHA;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiConstants;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.WatermarkData;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import java.util.Map;

public abstract class InMobiRewardedAd extends InterstitialAdEventListener
    implements MediationRewardedAd {

  private InMobiInterstitialWrapper inMobiRewardedAdWrapper;

  protected final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;
  protected final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;
  private InMobiInitializer inMobiInitializer;
  private InMobiAdFactory inMobiAdFactory;

  public InMobiRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiInitializer = inMobiInitializer;
    this.inMobiAdFactory = inMobiAdFactory;
  }

  /** Invokes the third-party method for loading the ad. */
  protected abstract void internalLoadAd(InMobiInterstitialWrapper inMobiRewardedAdWrapper);

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

    inMobiInitializer.init(context, accountID, new Listener() {
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
    if (!inMobiRewardedAdWrapper.isReady()) {
      AdError error = InMobiConstants.createAdapterError(ERROR_AD_NOT_READY,
          "InMobi rewarded ad is not yet ready to be shown.");
      Log.w(TAG, error.toString());

      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    inMobiRewardedAdWrapper.show();
  }
  // endregion

  // region Rewarded adapter utility classes.
  @VisibleForTesting
  public void createAndLoadRewardAd(Context context, long placementId,
      final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    inMobiRewardedAdWrapper = inMobiAdFactory.createInMobiInterstitialWrapper(context, placementId, InMobiRewardedAd.this);

    // Set the COPPA value in InMobi SDK.
    InMobiAdapterUtils.setIsAgeRestricted();

    InMobiAdapterUtils.configureGlobalTargeting(
        mediationRewardedAdConfiguration.getMediationExtras());

    String watermark = mediationRewardedAdConfiguration.getWatermark();
    if (!TextUtils.isEmpty(watermark)) {
      inMobiRewardedAdWrapper.setWatermarkData(new WatermarkData(watermark, WATERMARK_ALPHA));
    }

    internalLoadAd(inMobiRewardedAdWrapper);
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
    if (rewardedAdCallback != null) {
      Log.d(TAG, "InMobi rewarded ad credited the user with a reward.");
      rewardedAdCallback.onVideoComplete();
      rewardedAdCallback.onUserEarnedReward();
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
