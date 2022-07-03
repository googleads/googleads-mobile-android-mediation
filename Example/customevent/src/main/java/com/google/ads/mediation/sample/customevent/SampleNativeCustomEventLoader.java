/*
 * Copyright (C) 2015 Google, Inc.
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

import android.text.TextUtils;
import android.util.Log;
import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleNativeAd;
import com.google.ads.mediation.sample.sdk.SampleNativeAdListener;
import com.google.ads.mediation.sample.sdk.SampleNativeAdLoader;
import com.google.ads.mediation.sample.sdk.SampleNativeAdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdOptions;

/** Native custom event loader for the SampleSDK. */
public class SampleNativeCustomEventLoader extends SampleNativeAdListener {

  /** Configuration for requesting the native ad from the third party network. */
  private final MediationNativeAdConfiguration mediationNativeAdConfiguration;

  /** Callback that fires on loading success or failure. */
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      mediationAdLoadCallback;

  /** Tag used for log statements */
  private static final String TAG = "NativeCustomEvent";

  /**
   * Callback for native ad events. The usual link/click tracking handled through callback methods
   * are handled through the GMA SDK, described here:
   * https://developers.google.com/admob/android/custom-events/native#impression_and_click_events
   */
  @SuppressWarnings("unused")
  private MediationNativeAdCallback mediationNativeAdCallback;

  public SampleNativeCustomEventLoader(
      MediationNativeAdConfiguration mediationNativeAdConfiguration,
      MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          mediationAdLoadCallback) {
    this.mediationNativeAdConfiguration = mediationNativeAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  /** Loads the native ad from the third party ad network. */
  public void loadAd() {
    // Create one of the Sample SDK's ad loaders to request ads.
    Log.i(TAG, "Begin loading native ad.");
    SampleNativeAdLoader loader =
        new SampleNativeAdLoader(mediationNativeAdConfiguration.getContext());

    // All custom events have a server parameter named "parameter" that returns back the parameter
    // entered into the AdMob UI when defining the custom event.
    String serverParameter =
        mediationNativeAdConfiguration.getServerParameters().getString("parameter");
    if (TextUtils.isEmpty(serverParameter)) {
      mediationAdLoadCallback.onFailure(SampleCustomEventError.createCustomEventNoAdIdError());
      return;
    }
    Log.d(TAG, "Received server parameter.");

    loader.setAdUnit(serverParameter);

    // Create a native request to give to the SampleNativeAdLoader.
    SampleNativeAdRequest request = new SampleNativeAdRequest();
    NativeAdOptions options = mediationNativeAdConfiguration.getNativeAdOptions();
    if (options != null) {
      // If the NativeAdOptions' shouldReturnUrlsForImageAssets is true, the adapter should
      // send just the URLs for the images.
      request.setShouldDownloadImages(!options.shouldReturnUrlsForImageAssets());

      // If your network does not support any of the following options, please make sure
      // that it is documented in your adapter's documentation.
      request.setShouldDownloadMultipleImages(options.shouldRequestMultipleImages());
      switch (options.getMediaAspectRatio()) {
        case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE:
          request.setPreferredImageOrientation(SampleNativeAdRequest.IMAGE_ORIENTATION_LANDSCAPE);
          break;
        case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT:
          request.setPreferredImageOrientation(SampleNativeAdRequest.IMAGE_ORIENTATION_PORTRAIT);
          break;
        case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE:
        case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY:
        case NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_UNKNOWN:
        default:
          request.setPreferredImageOrientation(SampleNativeAdRequest.IMAGE_ORIENTATION_ANY);
      }
    }

    loader.setNativeAdListener(this);

    // Begin a request.
    Log.i(TAG, "Start fetching native ad.");
    loader.fetchAd(request);
  }

  /** Called when a native ad is successfully fetched. */
  @Override
  public void onNativeAdFetched(SampleNativeAd ad) {
    // If the mediated network only ever returns URLs for images, this is an appropriate place
    // to automatically download the image files if the publisher has indicated via the
    // NativeAdOptions object that the custom event should do so.
    //
    // For example, if the publisher set the NativeAdOption's shouldReturnUrlsForImageAssets
    // property to false, and the mediated network returns images only as URLs rather than
    // downloading them itself, the forwarder should:
    //
    // 1. Initiate HTTP downloads of the image assets from the returned URLs using Volley or
    //    another, similar mechanism.
    // 2. Wait for all the requests to complete.
    // 3. Give the mediated network's native ad object and the image assets to your mapper class
    //    (each custom event defines its own mapper classes, so you can add a parameter for this
    //    to the constructor.
    // 4. Call the MediationNativeListener's onAdLoaded method and give it a reference to your
    //    custom event and the mapped native ad, as seen below.
    //
    // The important thing is to make sure that the publisher's wishes in regard to automatic
    // image downloading are respected, and that any additional downloads take place *before*
    // the mapped native ad object is returned to the Google Mobile Ads SDK via the
    // onAdLoaded method.
    Log.d(TAG, "Received the native ad.");
    SampleUnifiedNativeAdMapper mapper = new SampleUnifiedNativeAdMapper(ad);
    mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(mapper);
  }

  @Override
  public void onAdFetchFailed(SampleErrorCode errorCode) {
    Log.e(TAG, "Failed to fetch the native ad.");
    mediationAdLoadCallback.onFailure(SampleCustomEventError.createSampleSdkError(errorCode));
  }

}
