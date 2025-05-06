/*
 * Copyright 2025 Google LLC
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
import com.google.ads.mediation.sample.sdk.SampleAppOpen;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;

/** App Open custom event loader for the SampleSDK. */
public class SampleAppOpenCustomEventLoader extends SampleAdListener implements MediationAppOpenAd {

  /** A sample third party SDK app open ad. */
  private SampleAppOpen sampleAppOpenAd;

  /** Configuration for requesting the app open ad from the third party network. */
  private final MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration;

  /** Callback that fires on loading success or failure. */
  private final MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
      mediationAdLoadCallback;

  /** Callback for app open ad events. */
  private MediationAppOpenAdCallback appOpenAdCallback;

  /** Tag used for log statements */
  private static final String TAG = "AppOpenCustomEvent";

  public SampleAppOpenCustomEventLoader(
      @NonNull MediationAppOpenAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
          mediationAdLoadCallback) {
    this.mediationAppOpenAdConfiguration = adConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Loads the app open ad from the third party ad network. */
  public void loadAd() {
    // All custom events have a server parameter named "parameter" that returns back the parameter
    // entered into the AdMob UI when defining the custom event.
    Log.i(TAG, "Begin loading app open ad.");
    String serverParameter =
        mediationAppOpenAdConfiguration.getServerParameters().getString("parameter");
    if (TextUtils.isEmpty(serverParameter)) {
      mediationAdLoadCallback.onFailure(SampleCustomEventError.createCustomEventNoAdIdError());
      return;
    }
    Log.d(TAG, "Received server parameter.");

    sampleAppOpenAd = new SampleAppOpen(mediationAppOpenAdConfiguration.getContext());
    sampleAppOpenAd.setAdUnit(serverParameter);

    // Implement a SampleAdListener and forward callbacks to mediation.
    sampleAppOpenAd.setAdListener(this);

    // Make an ad request.
    Log.i(TAG, "Start fetching an app open ad.");
    sampleAppOpenAd.fetchAd(SampleCustomEvent.createSampleRequest(mediationAppOpenAdConfiguration));
  }

  @Override
  public void showAd(@NonNull Context context) {
    Log.d(TAG, "Showing the app open ad.");
    sampleAppOpenAd.show();
  }

  // region SampleAdListener implementation
  @Override
  public void onAdFetchSucceeded() {
    Log.d(TAG, "Received the app open ad.");
    appOpenAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    Log.e(TAG, "Failed to fetch the app open ad with error code: " + errorCode);
    mediationAdLoadCallback.onFailure(SampleCustomEventError.createSampleSdkError(errorCode));
  }

  @Override
  public void onAdFullScreen() {
    Log.d(TAG, "The app open ad was shown fullscreen.");
    appOpenAdCallback.reportAdImpression();
    appOpenAdCallback.onAdOpened();
  }

  @Override
  public void onAdClosed() {
    Log.d(TAG, "The app open ad was closed.");
    appOpenAdCallback.onAdClosed();
  }
  // endregion
}
