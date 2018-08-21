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

import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleNativeAdListener;
import com.google.ads.mediation.sample.sdk.SampleNativeAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventNativeListener;

/**
 * A {@link SampleNativeAdListener} that forwards events to AdMob's
 * {@link CustomEventNativeListener}.
 */
public class SampleCustomNativeEventForwarder extends SampleNativeAdListener {
    private final CustomEventNativeListener nativeListener;
    private final NativeMediationAdRequest nativeAdRequest;

    /**
     * Creates a new {@code SampleNativeEventForwarder}.
     *
     * @param listener An AdMob Mediation {@link CustomEventNativeListener} that should receive
     *                 forwarded events.
     */
    public SampleCustomNativeEventForwarder(CustomEventNativeListener listener,
                                            NativeMediationAdRequest adRequest) {
        this.nativeListener = listener;
        this.nativeAdRequest = adRequest;
    }

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
        NativeAdOptions nativeAdOptions = nativeAdRequest.getNativeAdOptions();

        if (nativeAdRequest.isUnifiedNativeAdRequested()) {
            SampleUnifiedNativeAdMapper mapper =
                    new SampleUnifiedNativeAdMapper(ad, nativeAdOptions);
            nativeListener.onAdLoaded(mapper);
        } else if (containsRequiredAppInstallAdAssets(ad)) {
            SampleNativeAppInstallAdMapper mapper =
                    new SampleNativeAppInstallAdMapper(ad, nativeAdOptions);
            nativeListener.onAdLoaded(mapper);
        } else if (containsRequiredContentAdAssets(ad)) {
            SampleNativeContentAdMapper mapper =
                    new SampleNativeContentAdMapper(ad, nativeAdOptions);
            nativeListener.onAdLoaded(mapper);
        } else {
            // Each system-defined native ad format (App Install and Content) has a set of
            // "Always Included" assets. Mediated networks must check and fail the request if any
            // of the "Always Included" assets are not available for the ad loaded (the sample
            // SDK will always provide these assets, but this check is added as an example).
            nativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
        }

    }

    /**
     * This method will check whether or not he provided {@link SampleNativeAd} contains
     * all the required assets (headline, body, image, app icon and call to action) for it to be
     * mapped onto an AdMob native app install ad.
     *
     * @param appInstallAd the sample native app install ad to be checked.
     * @return {@code true} if the provided sample native app install ad contains all the
     * necessary assets for it to be mapped, {@code false} otherwise.
     */
    private boolean containsRequiredAppInstallAdAssets(SampleNativeAd appInstallAd) {
        return (appInstallAd != null && appInstallAd.getHeadline() != null
                && appInstallAd.getBody() != null && appInstallAd.getImage() != null
                && appInstallAd.getIcon() != null && appInstallAd.getCallToAction() != null);
    }

    /**
     * This method will check whether or not the provided {@link SampleNativeAd} contains
     * all the required assets (headline, body, image, call to action and advertiser) for it to be
     * mapped onto an AdMob native content ad.
     *
     * @param contentAd the sample native content ad to be checked.
     * @return {@code true} if the provided sample native content ad contains all the necessary
     * assets for it to be mapped, {@code false} otherwise.
     */
    private boolean containsRequiredContentAdAssets(SampleNativeAd contentAd) {
        return (contentAd != null && contentAd.getHeadline() != null && contentAd.getBody() != null
                && contentAd.getImage() != null && contentAd.getCallToAction() != null
                && contentAd.getAdvertiser() != null);
    }

    @Override
    public void onAdFetchFailed(SampleErrorCode errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                nativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                break;
            case BAD_REQUEST:
                nativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
            case NETWORK_ERROR:
                nativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NETWORK_ERROR);
                break;
            case NO_INVENTORY:
                nativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
                break;
        }
    }
}
