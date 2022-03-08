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
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleAdSize;
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/** Banner custom event loader for the SampleSDK. */
public class SampleBannerCustomEventLoader extends SampleAdListener implements MediationBannerAd {

  /** View to contain the sample banner ad. */
  private SampleAdView sampleAdView;

  /** Configuration for requesting the banner ad from the third party network. */
  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;

  /** Callback that fires on loading success or failure. */
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationAdLoadCallback;

  /** Callback for banner ad events. */
  private MediationBannerAdCallback bannerAdCallback;

  /** Tag used for log statements */
  private static final String TAG = "BannerCustomEvent";

  public SampleBannerCustomEventLoader(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Loads a banner ad from the third party ad network. */
  public void loadAd() {
    // All custom events have a server parameter named "parameter" that returns back the parameter
    // entered into the AdMob UI when defining the custom event.
    Log.i(TAG, "Begin loading banner ad.");
    String serverParameter =
        mediationBannerAdConfiguration.getServerParameters().getString("parameter");
    if (TextUtils.isEmpty(serverParameter)) {
      mediationAdLoadCallback.onFailure(SampleCustomEventError.createCustomEventNoAdIdError());
      return;
    }
    Log.d(TAG, "Received server parameter.");

    Context context = mediationBannerAdConfiguration.getContext();
    sampleAdView = new SampleAdView(context);

    // Assumes that the serverParameter is the AdUnit for the Sample Network.
    sampleAdView.setAdUnit(serverParameter);
    AdSize size = mediationBannerAdConfiguration.getAdSize();

    // Internally, smart banners use constants to represent their ad size, which means a call to
    // AdSize.getHeight could return a negative value. You can accommodate this by using
    // AdSize.getHeightInPixels and AdSize.getWidthInPixels instead, and then adjusting to match
    // the device's display metrics.
    int widthInPixels = size.getWidthInPixels(context);
    int heightInPixels = size.getHeightInPixels(context);
    DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    int widthInDp = Math.round(widthInPixels / displayMetrics.density);
    int heightInDp = Math.round(heightInPixels / displayMetrics.density);

    sampleAdView.setSize(new SampleAdSize(widthInDp, heightInDp));
    sampleAdView.setAdListener(this);

    SampleAdRequest request = SampleCustomEvent.createSampleRequest(mediationBannerAdConfiguration);
    Log.i(TAG, "Start fetching banner ad.");
    sampleAdView.fetchAd(request);
  }

  @Override
  public void onAdFetchSucceeded() {
    Log.d(TAG, "Received the banner ad.");
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this);
    bannerAdCallback.reportAdImpression();
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    Log.e(TAG, "Failed to fetch the banner ad.");
    mediationAdLoadCallback.onFailure(SampleCustomEventError.createSampleSdkError(errorCode));
  }

  @Override
  @NonNull
  public View getView() {
    return sampleAdView;
  }

  @Override
  public void onAdFullScreen() {
    Log.d(TAG, "The banner ad was clicked.");
    bannerAdCallback.onAdOpened();
    bannerAdCallback.onAdLeftApplication();
    bannerAdCallback.reportAdClicked();
  }

  @Override
  public void onAdClosed() {
    Log.d(TAG, "The banner ad was closed.");
    bannerAdCallback.onAdClosed();
  }
}
