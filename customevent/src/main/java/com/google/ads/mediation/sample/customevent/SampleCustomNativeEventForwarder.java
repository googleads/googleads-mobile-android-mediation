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
import com.google.ads.mediation.sample.sdk.SampleNativeAppInstallAd;
import com.google.ads.mediation.sample.sdk.SampleNativeContentAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventNativeListener;

/**
 * A {@link SampleNativeAdListener} that forwards events to AdMob's
 * {@link CustomEventNativeListener}.
 */
public class SampleCustomNativeEventForwarder extends SampleNativeAdListener {
    private CustomEventNativeListener mNativeListener;

    /**
     * Creates a new {@code SampleNativeEventForwarder}.
     *
     * @param listener An AdMob Mediation {@link CustomEventNativeListener} that should receive
     *                 forwarded events.
     */
    public SampleCustomNativeEventForwarder(CustomEventNativeListener listener) {
        this.mNativeListener = listener;
    }

    @Override
    public void onNativeAppInstallAdFetched(SampleNativeAppInstallAd ad) {
        SampleNativeAppInstallAdMapper mapper = new SampleNativeAppInstallAdMapper(ad);
        mNativeListener.onAdLoaded(mapper);
    }

    @Override
    public void onNativeContentAdFetched(SampleNativeContentAd ad) {
        SampleNativeContentAdMapper mapper = new SampleNativeContentAdMapper(ad);
        mNativeListener.onAdLoaded(mapper);
    }

    @Override
    public void onAdFetchFailed(SampleErrorCode errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                mNativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                break;
            case BAD_REQUEST:
                mNativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                break;
            case NETWORK_ERROR:
                mNativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NETWORK_ERROR);
                break;
            case NO_INVENTORY:
                mNativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
                break;
        }
    }
}
