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
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleMediaView;
import com.google.ads.mediation.sample.sdk.SampleMediaViewListener;
import com.google.ads.mediation.sample.sdk.SampleNativeAdListener;
import com.google.ads.mediation.sample.sdk.SampleNativeAdLoader;
import com.google.ads.mediation.sample.sdk.SampleNativeAdRequest;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdOptions;

public class SampleNativeAd extends SampleNativeAdListener implements SampleMediaViewListener {

  /**
   * Configurations used to load the native ad.
   */
  @NonNull
  private final MediationNativeAdConfiguration adConfiguration;

  /**
   * A {@link MediationAdLoadCallback} that handles any callback when a Sample native ad finishes
   * loading.
   */
  @NonNull
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      adLoadCallback;

  /**
   * Used to forward native ad events to the Google Mobile Ads SDK.
   */
  @Nullable
  private MediationNativeAdCallback nativeAdCallback;

  public SampleNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd() {
    /*
     * In this method, you should:
     *
     * 1. Create a SampleNativeAdLoader
     * 2. Set the native ad listener
     * 3. Set native ad options (optional assets)
     * 4. Make an ad request.
     */

    // Create the SampleNativeAdLoader.
    Context context = adConfiguration.getContext();
    SampleNativeAdLoader loader = new SampleNativeAdLoader(context);

    Bundle serverParameters = adConfiguration.getServerParameters();
    String sampleAdUnit = serverParameters.getString(SAMPLE_AD_UNIT_KEY);
    if (TextUtils.isEmpty(sampleAdUnit)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid ad unit.", ERROR_DOMAIN);
      adLoadCallback.onFailure(parameterError);
      return;
    }
    loader.setAdUnit(sampleAdUnit);

    NativeAdOptions nativeAdOptions = adConfiguration.getNativeAdOptions();

    // Implement a SampleNativeAdListener and forward callbacks to mediation.
    loader.setNativeAdListener(SampleNativeAd.this);

    SampleNativeAdRequest request = new SampleNativeAdRequest();
    // If the NativeAdOptions' `shouldReturnUrlsForImageAssets` is `true`, the adapter should
    // send just the URLs for the images.
    request.setShouldDownloadImages(!nativeAdOptions.shouldReturnUrlsForImageAssets());

    // If your network does not support any of the following options, please make sure
    // that it is documented in your adapter's documentation.
    request.setShouldDownloadMultipleImages(nativeAdOptions.shouldRequestMultipleImages());
    switch (nativeAdOptions.getMediaAspectRatio()) {
      case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE:
        request.setPreferredImageOrientation(
            SampleNativeAdRequest.IMAGE_ORIENTATION_LANDSCAPE);
        break;
      case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT:
        request.setPreferredImageOrientation(
            SampleNativeAdRequest.IMAGE_ORIENTATION_PORTRAIT);
        break;
      case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY:
      default:
        request.setPreferredImageOrientation(
            SampleNativeAdRequest.IMAGE_ORIENTATION_ANY);
    }

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
    loader.fetchAd(request);
  }

  // region SampleNativeAdListener implementation
  @Override
  public void onNativeAdFetched(
      @NonNull com.google.ads.mediation.sample.sdk.SampleNativeAd nativeAd) {
    // If your network only ever returns URLs for images, this is an appropriate place to
    // automatically download the image files if the publisher has indicated via the
    // NativeAdOptions object that the adapter should do so.
    //
    // For example, if the publisher set the NativeAdOption's shouldReturnUrlsForImageAssets
    // property to false, and your network returns images only as URLs rather than downloading
    // them itself, the forwarder should:
    //
    // 1. Initiate HTTP downloads of the image assets from the returned URLs using Volley or
    //    another, similar mechanism.
    // 2. Wait for all the requests to complete.
    // 3. Give your network's native ad object and the image assets to your mapper class (each
    //    adapter defines its own mapper classes, so you can add a parameter for this to the
    //    constructor.
    // 4. Call the MediationNativeListener's onAdLoaded method and give it a reference to your
    //    adapter and the mapped native ad, as seen below.
    //
    // The important thing is to make sure that the publisher's wishes in regard to automatic
    // image downloading are respected, and that any additional downloads take place *before*
    // the mapped native ad object is returned to the Google Mobile Ads SDK via the
    // onAdLoaded method.

    SampleMediaView mediaView = nativeAd.getMediaView();
    if (mediaView != null) {
      mediaView.setMediaViewListener(SampleNativeAd.this);
    }

    SampleNativeAdMapper mapper = new SampleNativeAdMapper(nativeAd);
    nativeAdCallback = adLoadCallback.onSuccess(mapper);
  }

  @Override
  public void onAdFetchFailed(@NonNull SampleErrorCode errorCode) {
    AdError loadError = new AdError(errorCode.ordinal(),
        "Sample SDK returned a failure callback.", SAMPLE_SDK_ERROR_DOMAIN);
    adLoadCallback.onFailure(loadError);
  }
  // endregion

  // region SampleMediaViewListener implementation.
  @Override
  public void onVideoEnd() {
    if (nativeAdCallback == null) {
      return;
    }

    nativeAdCallback.onVideoComplete();
  }
  // endregion

}
