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
import android.os.Bundle;
import androidx.annotation.Keep;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleAdSize;
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleInterstitial;
import com.google.ads.mediation.sample.sdk.SampleNativeAdLoader;
import com.google.ads.mediation.sample.sdk.SampleNativeAdRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.customevent.CustomEventBanner;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitial;
import com.google.android.gms.ads.mediation.customevent.CustomEventInterstitialListener;
import com.google.android.gms.ads.mediation.customevent.CustomEventNative;
import com.google.android.gms.ads.mediation.customevent.CustomEventNativeListener;

/**
 * A custom event for the Sample ad network. Custom events allow publishers to write their own
 * mediation adapter.
 *
 * Since the custom event is not directly referenced by the Google Mobile Ads SDK and is instead
 * instantiated with reflection, it's possible that ProGuard might remove it. Use the {@link Keep}}
 * annotation to make sure that the adapter is not removed when minifying the project.
 */
@Keep
public class SampleCustomEvent implements CustomEventBanner, CustomEventInterstitial,
        CustomEventNative {
    protected static final String TAG = SampleCustomEvent.class.getSimpleName();

    /**
     * Example of an extra field that publishers can use for a Native ad. In this example, the
     * String is added to a {@link Bundle} in {@link SampleUnifiedNativeAdMapper}.
     */
    public static final String DEGREE_OF_AWESOMENESS = "DegreeOfAwesomeness";

    /**
     * The pixel-to-dpi scale for images downloaded from the sample SDK's URL values. Scale value
     * is set in {@link SampleNativeMappedImage}.
     */
    public static final double SAMPLE_SDK_IMAGE_SCALE = 1.0;

    /**
     * The {@link SampleAdView} representing a banner ad.
     */
    private SampleAdView sampleAdView;

    /**
     * Represents a {@link SampleInterstitial}.
     */
    private SampleInterstitial sampleInterstitial;

    /**
     * The event is being destroyed. Perform any necessary cleanup here.
     */
    @Override
    public void onDestroy() {
        if (sampleAdView != null) {
            sampleAdView.destroy();
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

    @Override
    public void requestBannerAd(Context context,
                                CustomEventBannerListener listener,
                                String serverParameter,
                                AdSize size,
                                MediationAdRequest mediationAdRequest,
                                Bundle customEventExtras) {
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

        sampleAdView = new SampleAdView(context);

        // Assumes that the serverParameter is the AdUnit for the Sample Network.
        sampleAdView.setAdUnit(serverParameter);

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

        // Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
        // is handled by SampleBannerEventFowarder.
        sampleAdView.setAdListener(new SampleCustomBannerEventForwarder(listener, sampleAdView));

        // Make an ad request.
        sampleAdView.fetchAd(createSampleRequest(mediationAdRequest));
    }

    /**
     * Helper method to create a {@link SampleAdRequest}.
     *
     * @param mediationAdRequest The mediation request with targeting information.
     * @return The created {@link SampleAdRequest}.
     */
    private SampleAdRequest createSampleRequest(MediationAdRequest mediationAdRequest) {
        SampleAdRequest request = new SampleAdRequest();
        request.setTestMode(mediationAdRequest.isTesting());
        request.setKeywords(mediationAdRequest.getKeywords());
        return request;
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      CustomEventInterstitialListener listener,
                                      String serverParameter,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle customEventExtras) {
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

        sampleInterstitial = new SampleInterstitial(context);

        // Here we're assuming the serverParameter is the ad unit for the Sample Ad Network.
        sampleInterstitial.setAdUnit(serverParameter);

        // Implement a SampleAdListener and forward callbacks to mediation.
        sampleInterstitial.setAdListener(new SampleCustomInterstitialEventForwarder(listener));

        // Make an ad request.
        sampleInterstitial.fetchAd(createSampleRequest(mediationAdRequest));
    }

    @Override
    public void showInterstitial() {
        // Show your interstitial ad.
        sampleInterstitial.show();
    }

    @Override
    public void requestNativeAd(Context context,
                                CustomEventNativeListener customEventNativeListener,
                                String serverParameter,
                                NativeMediationAdRequest nativeMediationAdRequest,
                                Bundle extras) {
        // Create one of the Sample SDK's ad loaders from which to request ads.
        SampleNativeAdLoader loader = new SampleNativeAdLoader(context);
        loader.setAdUnit(serverParameter);

        // Create a native request to give to the SampleNativeAdLoader.
        SampleNativeAdRequest request = new SampleNativeAdRequest();
        NativeAdOptions options = nativeMediationAdRequest.getNativeAdOptions();
        if (options != null) {
            // If the NativeAdOptions' shouldReturnUrlsForImageAssets is true, the adapter should
            // send just the URLs for the images.
            request.setShouldDownloadImages(!options.shouldReturnUrlsForImageAssets());

            // If your network does not support any of the following options, please make sure
            // that it is documented in your adapter's documentation.
            request.setShouldDownloadMultipleImages(options.shouldRequestMultipleImages());
            switch (options.getImageOrientation()) {
                case NativeAdOptions.ORIENTATION_LANDSCAPE:
                    request.setPreferredImageOrientation(
                            SampleNativeAdRequest.IMAGE_ORIENTATION_LANDSCAPE);
                    break;
                case NativeAdOptions.ORIENTATION_PORTRAIT:
                    request.setPreferredImageOrientation(
                            SampleNativeAdRequest.IMAGE_ORIENTATION_PORTRAIT);
                    break;
                case NativeAdOptions.ORIENTATION_ANY:
                default:
                    request.setPreferredImageOrientation(
                            SampleNativeAdRequest.IMAGE_ORIENTATION_ANY);
            }
        }

        if (!nativeMediationAdRequest.isUnifiedNativeAdRequested()) {
            Log.e(TAG, "Failed to load ad. Request must be for unified native ads.");
            customEventNativeListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        loader.setNativeAdListener(
                new SampleCustomNativeEventForwarder(customEventNativeListener,
                        nativeMediationAdRequest.getNativeAdOptions()));

        // Begin a request.
        loader.fetchAd(request);
    }
}
