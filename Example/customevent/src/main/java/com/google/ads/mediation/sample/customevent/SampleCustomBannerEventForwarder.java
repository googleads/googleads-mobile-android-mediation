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
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;

/**
 * A {@link SampleAdListener} that forwards events to AdMob's {@link CustomEventBannerListener}.
 */
public class SampleCustomBannerEventForwarder extends SampleAdListener {

  private final CustomEventBannerListener bannerListener;
  private final SampleAdView adView;

  /**
   * Creates a new {@code SampleBannerEventForwarder}.
   *
   * @param listener An AdMob Mediation {@link CustomEventBannerListener} that should receive
   * forwarded events.
   * @param adView A {@link SampleAdView}.
   */
  public SampleCustomBannerEventForwarder(
      CustomEventBannerListener listener, SampleAdView adView) {
    this.bannerListener = listener;
    this.adView = adView;
  }

  @Override
  public void onAdFetchSucceeded() {
    bannerListener.onAdLoaded(adView);
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    AdError error = SampleCustomEventError.createSampleSdkError(errorCode);
    bannerListener.onAdFailedToLoad(error);
  }

  @Override
  public void onAdFullScreen() {
    bannerListener.onAdClicked();
    bannerListener.onAdOpened();
    // Only call onAdLeftApplication if your ad network actually exits the developer's app.
    bannerListener.onAdLeftApplication();
  }

  @Override
  public void onAdClosed() {
    bannerListener.onAdClosed();
  }
}
