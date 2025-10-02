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
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.mbridge.msdk.newinterstitial.out.NewInterstitialWithCodeListener;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.RewardInfo;

public abstract class MintegralInterstitialAd extends NewInterstitialWithCodeListener implements
    MediationInterstitialAd {

  protected final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;
  protected MediationInterstitialAdCallback interstitialAdCallback;

  protected final boolean muted;

  public MintegralInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    this.adLoadCallback = callback;
  }

  /** Loads a Mintegral Interstitial ad. */
  public abstract void loadAd(MediationInterstitialAdConfiguration adConfiguration);

  @Override
  public void onLoadCampaignSuccess(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onResourceLoadSuccess(MBridgeIds mBridgeIds) {
    interstitialAdCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onResourceLoadFailWithCode(MBridgeIds mBridgeIds, int errorCode,
      String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode, errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onAdShow(MBridgeIds mBridgeIds) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
      interstitialAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClose(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void onShowFailWithCode(MBridgeIds mBridgeIds, int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode, errorMessage);
    Log.w(TAG, error.toString());
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdClicked(MBridgeIds mBridgeIds) {
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onVideoComplete(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdCloseWithNIReward(MBridgeIds mBridgeIds, RewardInfo rewardInfo) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onEndcardShow(MBridgeIds mBridgeIds) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
