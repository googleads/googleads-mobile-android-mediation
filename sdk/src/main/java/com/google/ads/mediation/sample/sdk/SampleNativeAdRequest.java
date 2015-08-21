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

    private boolean mAppInstallAdsRequested;
    private boolean mContentAdsRequested;
    private boolean mShouldDownloadImages;

    public SampleNativeAdRequest() {
        super();
        mAppInstallAdsRequested = false;
        mContentAdsRequested = false;
        mShouldDownloadImages = true;
    }

    public boolean areAppInstallAdsRequested() {
        return mAppInstallAdsRequested;
    }

    public void setAppInstallAdsRequested(boolean appInstallAdsRequested) {
        this.mAppInstallAdsRequested = appInstallAdsRequested;
    }

    public boolean areContentAdsRequested() {
        return mContentAdsRequested;
    }

    public void setContentAdsRequested(boolean contentAdsRequested) {
        this.mContentAdsRequested = contentAdsRequested;
    }

    public boolean getShouldDownloadImages() {
        return mShouldDownloadImages;
    }

    public void setShouldDownloadImages(boolean shouldDownloadImages) {
        this.mShouldDownloadImages = shouldDownloadImages;
    }
}
