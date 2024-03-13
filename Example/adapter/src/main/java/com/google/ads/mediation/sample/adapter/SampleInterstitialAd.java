// Copyright 2024 Google LLC
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

import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.SAMPLE_AD_UNIT_KEY;
import static com.google.ads.mediation.sample.adapter.SampleAdapter.SAMPLE_SDK_ERROR_DOMAIN;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.sample.adapter.SampleAdapter.MediationExtrasBundleBuilder;
import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleInterstitial;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class SampleInterstitialAd extends SampleAdListener implements MediationInterstitialAd {

  /**
   * Configurations used to load the interstitial ad.
   */
  @NonNull
  private final MediationInterstitialAdConfiguration adConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample interstitial ad
   * finishes loading.
   */
  @NonNull
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;

  /**
   * Used to forward interstitial ad events to the Google Mobile Ads SDK.
   */
  @Nullable
  private MediationInterstitialAdCallback interstitialAdCallback;

  /**
   * Sample SDK interstitial ad object.
   */
  private SampleInterstitial sampleInterstitial;

  public SampleInterstitialAd(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    /*
     * In this method, you should:
     *
     * 1. Create your interstitial ad.
     * 2. Set your ad network's listener.
     * 3. Make an ad request.
     */

    // Create the SampleInterstitial.
    Context context = adConfiguration.getContext();
    sampleInterstitial = new SampleInterstitial(context);

    Bundle serverParameters = adConfiguration.getServerParameters();
    String sampleAdUnit = serverParameters.getString(SAMPLE_AD_UNIT_KEY);
    if (TextUtils.isEmpty(sampleAdUnit)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid ad unit.", ERROR_DOMAIN);
      adLoadCallback.onFailure(parameterError);
      return;
    }
    sampleInterstitial.setAdUnit(sampleAdUnit);

    // Implement a SampleAdListener and forward callbacks to mediation.
    sampleInterstitial.setAdListener(SampleInterstitialAd.this);

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
    sampleInterstitial.fetchAd(request);
  }

  @Override
  public void showAd(@NonNull Context context) {
    // Show the interstitial ad.
    if (sampleInterstitial != null) {
      sampleInterstitial.show();
    }
  }

  // region SampleAdListener implementation
  @Override
  public void onAdFetchSucceeded() {
    interstitialAdCallback = adLoadCallback.onSuccess(SampleInterstitialAd.this);
  }

  @Override
  public void onAdFetchFailed(@NonNull SampleErrorCode errorCode) {
    AdError loadError = new AdError(errorCode.ordinal(),
        "Sample SDK returned a failure callback.", SAMPLE_SDK_ERROR_DOMAIN);
    adLoadCallback.onFailure(loadError);
  }

  @Override
  public void onAdFullScreen() {
    if (interstitialAdCallback == null) {
      return;
    }

    interstitialAdCallback.onAdOpened();
    // Only call `onAdLeftApplication()` if your ad network actually causes the user to leave the
    // application.
    interstitialAdCallback.onAdLeftApplication();
  }

  @Override
  public void onAdClosed() {
    if (interstitialAdCallback == null) {
      return;
    }

    interstitialAdCallback.onAdClosed();
  }
  // endregion

}
