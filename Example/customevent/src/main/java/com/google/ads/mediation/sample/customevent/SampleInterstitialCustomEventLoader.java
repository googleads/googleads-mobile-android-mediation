/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.customevent;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleInterstitial;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/** Interstitial custom event loader for the SampleSDK. */
public class SampleInterstitialCustomEventLoader extends SampleAdListener
    implements MediationInterstitialAd {

  /** A sample third party SDK interstitial ad. */
  private SampleInterstitial sampleInterstitialAd;

  /** Configuration for requesting the interstitial ad from the third party network. */
  private final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;

  /** Callback that fires on loading success or failure. */
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;

  /** Callback for interstitial ad events. */
  private MediationInterstitialAdCallback interstitialAdCallback;

  /** Tag used for log statements */
  private static final String TAG = "InterstitialCustomEvent";

  public SampleInterstitialCustomEventLoader(
      MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Loads the interstitial ad from the third party ad network. */
  public void loadAd() {
    // All custom events have a server parameter named "parameter" that returns back the parameter
    // entered into the AdMob UI when defining the custom event.
    Log.i(TAG, "Begin loading interstitial ad.");
    String serverParameter =
        mediationInterstitialAdConfiguration.getServerParameters().getString("parameter");
    if (TextUtils.isEmpty(serverParameter)) {
      mediationAdLoadCallback.onFailure(SampleCustomEventError.createCustomEventNoAdIdError());
      return;
    }
    Log.d(TAG, "Received server parameter.");

    sampleInterstitialAd =
        new SampleInterstitial(mediationInterstitialAdConfiguration.getContext());
    sampleInterstitialAd.setAdUnit(serverParameter);

    // Implement a SampleAdListener and forward callbacks to mediation.
    sampleInterstitialAd.setAdListener(this);

    // Make an ad request.
    Log.i(TAG, "start fetching interstitial ad.");
    sampleInterstitialAd.fetchAd(
        SampleCustomEvent.createSampleRequest(mediationInterstitialAdConfiguration));
  }

  @Override
  public void onAdFetchSucceeded() {
    Log.d(TAG, "Received the interstitial ad.");
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    Log.e(TAG, "Failed to fetch the interstitial ad.");
    mediationAdLoadCallback.onFailure(SampleCustomEventError.createSampleSdkError(errorCode));
  }

  @Override
  public void onAdFullScreen() {
    Log.d(TAG, "The interstitial ad was shown fullscreen.");
    interstitialAdCallback.reportAdImpression();
    interstitialAdCallback.onAdOpened();
  }

  @Override
  public void onAdClosed() {
    Log.d(TAG, "The interstitial ad was closed.");
    interstitialAdCallback.onAdClosed();
  }

  @Override
  public void showAd(@NonNull Context context) {
    Log.d(TAG, "The interstitial ad was shown.");
    sampleInterstitialAd.show();
  }
}
