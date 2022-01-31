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

import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;

/**
 * A {@link SampleAdListener} that forwards events to AdMob Mediation's {@link
 * CustomEventInterstitialListener}.
 */
public class SampleCustomInterstitialEventForwarder extends SampleAdListener {

  private final CustomEventInterstitialListener interstitialListener;

  /**
   * Creates a new {@code SampleInterstitialEventForwarder}.
   *
   * @param listener An AdMob Mediation {@link CustomEventInterstitialListener} that should receive
   * forwarded events.
   */
  public SampleCustomInterstitialEventForwarder(CustomEventInterstitialListener listener) {
    this.interstitialListener = listener;
  }

  @Override
  public void onAdFetchSucceeded() {
    interstitialListener.onAdLoaded();
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    AdError error = SampleCustomEventError.createSampleSdkError(errorCode);
    interstitialListener.onAdFailedToLoad(error);
  }

  @Override
  public void onAdFullScreen() {
    interstitialListener.onAdOpened();
    // Only call onAdLeftApplication if your ad network actually exits the developer's app.
    interstitialListener.onAdLeftApplication();
  }

  @Override
  public void onAdClosed() {
    interstitialListener.onAdClosed();
  }
}
