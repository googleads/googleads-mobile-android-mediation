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

package com.google.ads.mediation.sample.adapter;

import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleAdSize;
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleInterstitial;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

/**
 * A mediation adapter for the Sample ad network. This class can be used as a reference to help
 * other ad networks build their own mediation adapter.
 * <p/>
 * NOTE: The audience for this sample is mediation ad networks who are trying to build an ad network
 * adapter, not an app developer trying to integrate Google Mobile Ads into their application.
 */
public class SampleAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {
    /**
     * Your network probably depends on one or more identifiers that publishers need to provide.
     * Create the keys that your require. For AdMob, only an ad unit ID is required. The key(s) can
     * be whatever you'd prefer. They will be configured on the AdMob front-end later.
     * <p/>
     * Once the AdMob front-end is appropriately configured, the publisher will enter the key/value
     * pair(s) that you require. When your adapter is invoked, you will be provided a {@link Bundle}
     * in {@link #requestBannerAd(Context, MediationBannerListener, Bundle, AdSize,
     * MediationAdRequest, Bundle)} and {@link #requestInterstitialAd(Context,
     * MediationInterstitialListener, Bundle, MediationAdRequest, Bundle)} populated with the
     * expected key/value pair(s) from the server. These value(s) should be used to make an ad
     * request.
     */
    private static final String SAMPLE_AD_UNIT_KEY = "ad_unit";

    /**
     * The {@link SampleAdView} representing a banner ad.
     */
    private SampleAdView mSampleAdView;

    /**
     * Represents a {@link SampleInterstitial}.
     */
    private SampleInterstitial mSampleInterstitial;

    /**
     * The adapter is being destroyed. Perform any necessary cleanup here.
     */
    @Override
    public void onDestroy() {
        if (mSampleAdView != null) {
            mSampleAdView.destroy();
        }
    }

    /**
     * The app is being paused. This call will only be forwarded to the adapter if the developer
     * notifies mediation that the app is being paused.
     */
    @Override
    public void onPause() {
        // The sample ad network doesn't have an onPause method, so it does nothing.
    }

    /**
     * The app is being resumed. This call will only be forwarded to the adapter if the developer
     * notifies mediation that the app is being resumed.
     */
    @Override
    public void onResume() {
        // The sample ad network doesn't have an onResume method, so it does nothing.
    }

    /**
     * This method will only ever be called once per adapter instance.
     */
    @Override
    public void requestBannerAd(
            Context context,
            MediationBannerListener listener,
            Bundle serverParameters,
            AdSize adSize,
            MediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {
        /*
         * In this method, you should:
         *
         * 1. Create your banner view.
         * 2. Set your ad network's listener.
         * 3. Make an ad request.
         *
         * When setting your ad network's listener, don't forget to send the following callbacks:
         *
         * listener.onAdLoaded(this);
         * listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_*);
         * listener.onAdClicked(this);
         * listener.onAdOpened(this);
         * listener.onAdLeftApplication(this);
         * listener.onAdClosed(this);
         */

        // Create the SampleAdView.
        mSampleAdView = new SampleAdView(context);

        if (serverParameters.containsKey(SAMPLE_AD_UNIT_KEY)) {
            mSampleAdView.setAdUnit(serverParameters.getString(SAMPLE_AD_UNIT_KEY));
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }

        mSampleAdView.setSize(new SampleAdSize(adSize.getWidth(), adSize.getHeight()));

        // Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
        // is handled by SampleBannerEventFowarder.
        mSampleAdView.setAdListener(new SampleMediationBannerEventForwarder(listener, this));

        // Make an ad request.
        mSampleAdView.fetchAd(createSampleRequest(mediationAdRequest));
    }

    /**
     * Helper method to create a {@link SampleAdRequest}.
     *
     * @param mediationAdRequest The mediation request with targeting information.
     * @return The created {@link SampleAdRequest}.
     */
    public SampleAdRequest createSampleRequest(MediationAdRequest mediationAdRequest) {
        SampleAdRequest request = new SampleAdRequest();
        request.setTestMode(mediationAdRequest.isTesting());
        request.setKeywords(mediationAdRequest.getKeywords());
        return request;
    }

    @Override
    public View getBannerView() {
        // Return the banner view that you created from requestBannerAd().
        return mSampleAdView;
    }

    /**
     * This method will only ever be called once per adapter instance.
     */
    @Override
    public void requestInterstitialAd(
            Context context,
            MediationInterstitialListener listener,
            Bundle serverParameters,
            MediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {
        /*
         * In this method, you should:
         *
         * 1. Create your interstitial ad.
         * 2. Set your ad network's listener.
         * 3. Make an ad request.
         *
         * When setting your ad network's listener, don't forget to send the following callbacks:
         *
         * listener.onAdLoaded(this);
         * listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_*);
         * listener.onAdOpened(this);
         * listener.onAdLeftApplication(this);
         * listener.onAdClosed(this);
         */

        // Create the SampleInterstitial.
        mSampleInterstitial = new SampleInterstitial(context);

        if (serverParameters.containsKey(SAMPLE_AD_UNIT_KEY)) {
            mSampleInterstitial.setAdUnit(serverParameters.getString(SAMPLE_AD_UNIT_KEY));
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }

        // Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
        // is handled by SampleInterstitialEventFowarder.
        mSampleInterstitial.setAdListener(
                new SampleMediationInterstitialEventForwarder(listener, this));

        // Make an ad request.
        mSampleInterstitial.fetchAd(createSampleRequest(mediationAdRequest));
    }

    @Override
    public void showInterstitial() {
        // Show your interstitial ad.
        mSampleInterstitial.show();
    }

}
