// Copyright 2020 Google LLC
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

package com.google.ads.mediation.sample.adapter;

import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_AD_NOT_AVAILABLE;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_CONTEXT_NOT_ACTIVITY;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.SAMPLE_AD_UNIT_KEY;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.SAMPLE_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.ads.mediation.sample.adapter.SampleAdapter.MediationExtrasBundleBuilder;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleRewardedAdListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public class SampleRewardedAd extends SampleRewardedAdListener implements MediationRewardedAd {

  /**
   * Configurations used to load the rewarded ad.
   */
  private final MediationRewardedAdConfiguration adConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample rewarded ad finishes
   * loading.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;

  /**
   * Used to forward rewarded ad events to the Google Mobile Ads SDK.
   */
  private MediationRewardedAdCallback rewardedAdCallback;

  /**
   * Sample SDK rewarded ad object.
   */
  private com.google.ads.mediation.sample.sdk.SampleRewardedAd sampleRewardedAd;

  public SampleRewardedAd(@NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    /*
     * In this method, you should:
     *
     * 1. Create your rewarded ad.
     * 2. Set your ad network's listener.
     * 3. Make an ad request.
     */

    // Create the SampleRewardedAd.
    Bundle serverParameters = adConfiguration.getServerParameters();
    String sampleAdUnit = serverParameters.getString(SAMPLE_AD_UNIT_KEY);
    if (TextUtils.isEmpty(sampleAdUnit)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid ad unit.", ERROR_DOMAIN);
      adLoadCallback.onFailure(parameterError);
      return;
    }
    sampleRewardedAd = new com.google.ads.mediation.sample.sdk.SampleRewardedAd(sampleAdUnit);

    // Implement a SampleRewardedAdListener and forward callbacks to mediation.
    sampleRewardedAd.setListener(SampleRewardedAd.this);

    SampleAdRequest request = new SampleAdRequest();
    request.setTestMode(adConfiguration.isTestRequest());

    // If your network supports additional request parameters, the publisher can send these
    // additional parameters to the adapter using the `mediationExtras` bundle.
    // Creating a bundle builder class makes it easier for the publisher to create this bundle.
    Bundle mediationExtras = adConfiguration.getMediationExtras();
    if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE)) {
      request.setShouldAddAwesomeSauce(
          mediationExtras.getBoolean(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE));
    }
    if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_INCOME)) {
      request.setIncome(mediationExtras.getInt(MediationExtrasBundleBuilder.KEY_INCOME));
    }

    // Make an ad request.
    sampleRewardedAd.loadAd(request);
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (sampleRewardedAd == null) {
      return;
    }

    if (!sampleRewardedAd.isAdAvailable()) {
      AdError availabilityError = new AdError(ERROR_AD_NOT_AVAILABLE, "No ads to show.",
          ERROR_DOMAIN);
      rewardedAdCallback.onAdFailedToShow(availabilityError);
      return;
    }

    if (!(context instanceof Activity)) {
      AdError contextError = new AdError(ERROR_CONTEXT_NOT_ACTIVITY,
          "An Activity context is required to show Sample rewarded ad.", ERROR_DOMAIN);
      rewardedAdCallback.onAdFailedToShow(contextError);
      return;
    }

    Activity activity = (Activity) context;
    sampleRewardedAd.showAd(activity);
  }

  // region SampleRewardedAdListener implementation
  @Override
  public void onRewardedAdLoaded() {
    rewardedAdCallback = adLoadCallback.onSuccess(SampleRewardedAd.this);
  }

  @Override
  public void onRewardedAdFailedToLoad(@NonNull SampleErrorCode error) {
    AdError loadError = new AdError(error.ordinal(),
        "Sample SDK returned a failure callback.", SAMPLE_SDK_ERROR_DOMAIN);
    adLoadCallback.onFailure(loadError);
  }

  @Override
  public void onAdRewarded(@NonNull final String rewardType, final int amount) {
    if (rewardedAdCallback == null) {
      return;
    }

    SampleRewardItem rewardItem = new SampleRewardItem(rewardType, amount);
    rewardedAdCallback.onUserEarnedReward(rewardItem);
  }

  @Override
  public void onAdClicked() {
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.reportAdClicked();
  }

  @Override
  public void onAdFullScreen() {
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.onAdOpened();
    rewardedAdCallback.onVideoStart();
    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onAdClosed() {
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.onAdClosed();
  }

  @Override
  public void onAdCompleted() {
    if (rewardedAdCallback == null) {
      return;
    }

    rewardedAdCallback.onVideoComplete();
  }
  // endregion

}
