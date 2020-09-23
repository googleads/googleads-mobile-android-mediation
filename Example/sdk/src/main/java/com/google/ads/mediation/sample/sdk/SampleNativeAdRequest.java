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

package com.google.ads.mediation.sample.sdk;

/**
 * An example request class for native ads that can be used with {@link SampleNativeAdLoader}.
 */
public class SampleNativeAdRequest extends SampleAdRequest {

  public static final int IMAGE_ORIENTATION_ANY = 0;
  public static final int IMAGE_ORIENTATION_PORTRAIT = 1;
  public static final int IMAGE_ORIENTATION_LANDSCAPE = 2;

  private boolean shouldDownloadImages;

  public SampleNativeAdRequest() {
    super();
    shouldDownloadImages = true;
  }

  public boolean getShouldDownloadImages() {
    return shouldDownloadImages;
  }

  public void setShouldDownloadImages(boolean shouldDownloadImages) {
    this.shouldDownloadImages = shouldDownloadImages;
  }

  // For the sake of simplicity, the following two values are ignored by the Sample SDK.
  // They're included so that the custom event and adapter classes can demonstrate how to take
  // a request from the Google Mobile Ads SDK and translate it into one for the Sample SDK.
  public void setShouldDownloadMultipleImages(boolean shouldDownloadMultipleImages) {

  }

  public void setPreferredImageOrientation(int preferredImageOrientation) {

  }
}
