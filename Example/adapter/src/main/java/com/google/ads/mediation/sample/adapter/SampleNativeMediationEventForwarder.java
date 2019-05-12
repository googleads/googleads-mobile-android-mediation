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

package com.google.ads.mediation.sample.adapter;

import com.google.ads.mediation.sample.sdk.SampleErrorCode;
import com.google.ads.mediation.sample.sdk.SampleMediaView;
import com.google.ads.mediation.sample.sdk.SampleNativeAd;
import com.google.ads.mediation.sample.sdk.SampleNativeAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeListener;

/**
 * A {@link SampleNativeAdListener} that forwards events to AdMob's
 * {@link MediationNativeListener}.
 */
public class SampleNativeMediationEventForwarder extends SampleNativeAdListener {
    private final MediationNativeListener nativeListener;
    private final SampleAdapter adapter;
    private final NativeAdOptions nativeAdOptions;

    /**
     * Creates a new {@code SampleNativeMediationEventForwarder}.
     *
     * @param listener An AdMob Mediation {@link MediationNativeListener} that should receive
     *                 forwarded events.
     *
     * @param adapter A {@link SampleAdapter} mediation adapter.
     * @param options Native ad loading options.
     */
    public SampleNativeMediationEventForwarder(
            MediationNativeListener listener, SampleAdapter adapter, NativeAdOptions options) {
        this.nativeListener = listener;
        this.adapter = adapter;
        this.nativeAdOptions = options;
    }

    @Override
    public void onNativeAdFetched(SampleNativeAd ad) {
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

        SampleMediaView mediaView = ad.getMediaView();
        if (mediaView != null) {
            mediaView.setMediaViewListener(
                    new SampleMediaViewEventForwarder(nativeListener, adapter, mediaView));
        }

        SampleUnifiedNativeAdMapper mapper = new SampleUnifiedNativeAdMapper(ad);
        nativeListener.onAdLoaded(adapter, mapper);
    }

    @Override
    public void onAdFetchFailed(SampleErrorCode errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                break;
            case BAD_REQUEST:
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
            case NETWORK_ERROR:
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NETWORK_ERROR);
                break;
            case NO_INVENTORY:
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NO_FILL);
                break;
        }
    }
}
