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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.sample.adapter.SampleAdapter.MediationExtrasBundleBuilder;
import com.google.ads.mediation.sample.sdk.SampleAdListener;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleAdSize;
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class SampleBannerAd extends SampleAdListener implements MediationBannerAd {

  /**
   * Configurations used to load the banner ad.
   */
  @NonNull
  private final MediationBannerAdConfiguration adConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample banner ad finishes
   * loading.
   */
  @NonNull
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;

  /**
   * Used to forward banner ad events to the Google Mobile Ads SDK.
   */
  @Nullable
  private MediationBannerAdCallback bannerAdCallback;

  /**
   * Sample SDK banner ad object.
   */
  private SampleAdView sampleAdView;

  public SampleBannerAd(@NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    /*
     * In this method, you should:
     *
     * 1. Create your banner view.
     * 2. Set your ad network's listener.
     * 3. Make an ad request.
     */

    // Create the SampleAdView.
    Context context = adConfiguration.getContext();
    sampleAdView = new SampleAdView(context);

    Bundle serverParameters = adConfiguration.getServerParameters();
    String sampleAdUnit = serverParameters.getString(SAMPLE_AD_UNIT_KEY);
    if (TextUtils.isEmpty(sampleAdUnit)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid ad unit.", ERROR_DOMAIN);
      adLoadCallback.onFailure(parameterError);
      return;
    }
    sampleAdView.setAdUnit(sampleAdUnit);

    // Internally, smart banners use constants to represent their ad size, which means a call to
    // AdSize.getHeight could return a negative value. You can accommodate this by using
    // AdSize.getHeightInPixels and AdSize.getWidthInPixels instead, and then adjusting to match
    // the device's display metrics.
    AdSize requestedAdSize = adConfiguration.getAdSize();
    int widthInPixels = requestedAdSize.getWidthInPixels(context);
    int heightInPixels = requestedAdSize.getHeightInPixels(context);
    DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    int widthInDp = Math.round(widthInPixels / displayMetrics.density);
    int heightInDp = Math.round(heightInPixels / displayMetrics.density);
    sampleAdView.setSize(new SampleAdSize(widthInDp, heightInDp));

    // Implement a SampleAdListener and forward callbacks to mediation.
    sampleAdView.setAdListener(SampleBannerAd.this);

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
    sampleAdView.fetchAd(request);
  }

  @NonNull
  @Override
  public View getView() {
    // Return the banner view that you created from loadAd().
    return sampleAdView;
  }

  // region SampleAdListener implementation
  @Override
  public void onAdFetchSucceeded() {
    bannerAdCallback = adLoadCallback.onSuccess(SampleBannerAd.this);
  }

  @Override
  public void onAdFetchFailed(@NonNull SampleErrorCode errorCode) {
    AdError loadError = new AdError(errorCode.ordinal(),
        "Sample SDK returned a failure callback.", SAMPLE_SDK_ERROR_DOMAIN);
    adLoadCallback.onFailure(loadError);
  }

  @Override
  public void onAdFullScreen() {
    if (bannerAdCallback == null) {
      return;
    }

    bannerAdCallback.reportAdClicked();
    bannerAdCallback.onAdOpened();

    // Only call `onAdLeftApplication()` if your ad network actually causes the user to leave the
    // application.
    bannerAdCallback.onAdLeftApplication();
  }

  @Override
  public void onAdClosed() {
    if (bannerAdCallback == null) {
      return;
    }

    bannerAdCallback.onAdClosed();
  }
  // endregion

}
