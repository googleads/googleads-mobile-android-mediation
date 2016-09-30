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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.ads.mediation.sample.sdk.SampleAdSize;
import com.google.ads.mediation.sample.sdk.SampleAdView;
import com.google.ads.mediation.sample.sdk.SampleInterstitial;
import com.google.ads.mediation.sample.sdk.SampleNativeAdLoader;
import com.google.ads.mediation.sample.sdk.SampleNativeAdRequest;
import com.google.ads.mediation.sample.sdk.SampleRewardedVideo;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * A mediation adapter for the Sample ad network. This class can be used as a reference to help
 * other ad networks build their own mediation adapter.
 * <p/>
 * NOTE: The audience for this sample is mediation ad networks who are trying to build an ad network
 * adapter, not an app developer trying to integrate Google Mobile Ads into their application.
 */
public class SampleAdapter implements MediationBannerAdapter, MediationInterstitialAdapter,
        MediationNativeAdapter, MediationRewardedVideoAdAdapter, OnContextChangedListener {
    protected static final String TAG = SampleAdapter.class.getSimpleName();

    /**
     * Example of an extra field that publishers can use for a Native ad. In this example, the
     * String is added to a {@link Bundle} in {@link SampleNativeAppInstallAdMapper} and
     * {@link SampleNativeContentAdMapper}.
     */
    public static final String DEGREE_OF_AWESOMENESS = "DegreeOfAwesomeness";

    /**
     * The pixel-to-dpi scale for images downloaded from the sample SDK's URL values. Scale value
     * is set in {@link SampleNativeMappedImage}.
     */
    public static final double SAMPLE_SDK_IMAGE_SCALE = 1.0;

    /**
     * Your network probably depends on one or more identifiers that publishers need to provide.
     * Create the keys that your require. For AdMob, only an ad unit ID is required. The key(s) can
     * be whatever you'd prefer. They will be configured on the AdMob front-end later.
     * <p/>
     * Once the AdMob front-end is appropriately configured, the publisher will enter the key/value
     * pair(s) that you require. When your adapter is invoked, you will be provided a {@link Bundle}
     * in {@link #requestBannerAd(Context, MediationBannerListener, Bundle, AdSize,
     * MediationAdRequest, Bundle)}, {@link #requestInterstitialAd(Context,
     * MediationInterstitialListener, Bundle, MediationAdRequest, Bundle)} and
     * {@link #requestNativeAd(Context, MediationNativeListener, Bundle, NativeMediationAdRequest,
     * Bundle)} populated with the expected key/value pair(s) from the server. These value(s) should
     * be used to make an ad request.
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
     * Used to forward rewarded video ad events to AdMob.
     */
    private SampleMediationRewardedVideoEventForwarder mRewardedVideoEventForwarder;

    /**
     * The adapter is being destroyed. Perform any necessary cleanup here.
     */
    @Override
    public void onDestroy() {
        if (mSampleAdView != null) {
            mSampleAdView.destroy();
        }

        if (mRewardedVideoEventForwarder != null) {
            SampleRewardedVideo.destroy();
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
         */

        // Create the SampleAdView.
        mSampleAdView = new SampleAdView(context);

        if (serverParameters.containsKey(SAMPLE_AD_UNIT_KEY)) {
            mSampleAdView.setAdUnit(serverParameters.getString(SAMPLE_AD_UNIT_KEY));
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }

        // Internally, smart banners use constants to represent their ad size, which means a call to
        // AdSize.getHeight could return a negative value. You can accommodate this by using
        // AdSize.getHeightInPixels and AdSize.getWidthInPixels instead, and then adjusting to match
        // the device's display metrics.
        int widthInPixels = adSize.getWidthInPixels(context);
        int heightInPixels = adSize.getHeightInPixels(context);
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int widthInDp = Math.round(widthInPixels / displayMetrics.density);
        int heightInDp = Math.round(heightInPixels / displayMetrics.density);

        mSampleAdView.setSize(new SampleAdSize(widthInDp, heightInDp));

        /**
         * Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
         * is handled by {@link SampleMediationBannerEventForwarder}.
         */
        mSampleAdView.setAdListener(new SampleMediationBannerEventForwarder(listener, this));

        SampleAdRequest request = createSampleRequest(mediationAdRequest);

        /**
         * If your network supports additional request parameters, the publisher can send these
         * additional parameters to the adapter using the {@link mediationExtras} bundle.
         * Creating a bundle builder class makes it easier for the publisher to create this bundle.
         */
        if (mediationExtras != null) {
            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE)) {
                request.setShouldAddAwesomeSauce(
                        mediationExtras.getBoolean(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE));
            }

            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_INCOME)) {
                request.setIncome(mediationExtras.getInt(MediationExtrasBundleBuilder.KEY_INCOME));
            }
        }

        // Make an ad request.
        mSampleAdView.fetchAd(request);
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
         */

        // Create the SampleInterstitial.
        mSampleInterstitial = new SampleInterstitial(context);

        if (serverParameters.containsKey(SAMPLE_AD_UNIT_KEY)) {
            mSampleInterstitial.setAdUnit(serverParameters.getString(SAMPLE_AD_UNIT_KEY));
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
        }

        /**
         * Implement a SampleAdListener and forward callbacks to mediation. The callback forwarding
         * is handled by {@link SampleMediationInterstitialEventForwarder}.
         */
        mSampleInterstitial.setAdListener(
                new SampleMediationInterstitialEventForwarder(listener, this));

        SampleAdRequest request = createSampleRequest(mediationAdRequest);

        /**
         * If your network supports additional request parameters, the publisher can send these
         * additional parameters to the adapter using the {@link mediationExtras} bundle.
         * Creating a bundle builder class makes it easier for the publisher to create this bundle.
         */
        if (mediationExtras != null) {
            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE)) {
                request.setShouldAddAwesomeSauce(
                        mediationExtras.getBoolean(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE));
            }

            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_INCOME)) {
                request.setIncome(mediationExtras.getInt(MediationExtrasBundleBuilder.KEY_INCOME));
            }
        }

        // Make an ad request.
        mSampleInterstitial.fetchAd(request);
    }

    @Override
    public void showInterstitial() {
        // Show the interstitial ad.
        mSampleInterstitial.show();
    }

    @Override
    public void requestNativeAd(Context context,
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        /*
         * In this method, you should:
         *
         * 1. Create a SampleNativeAdLoader
         * 2. Set the native ad listener
         * 3. Set native ad options (optional assets)
         * 4. Make an ad request.
         */

        SampleNativeAdLoader loader = new SampleNativeAdLoader(context);
        if (serverParameters.containsKey(SAMPLE_AD_UNIT_KEY)) {
            loader.setAdUnit(serverParameters.getString(SAMPLE_AD_UNIT_KEY));
        } else {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        /**
         * Set the native ad listener and forward callbacks to mediation. The callback forwarding
         * is handled by {@link SampleNativeMediationEventForwarder}.
         */
        loader.setNativeAdListener(new SampleNativeMediationEventForwarder(listener, this));
        SampleNativeAdRequest request = new SampleNativeAdRequest();

        // The Google Mobile Ads SDK requires the image assets to be downloaded automatically unless
        // the publisher specifies otherwise by using the NativeAdOptions object's
        // shouldReturnUrlsForImageAssets method. If your network doesn't have an option like this
        // and instead only ever returns URLs for images (rather than the images themselves), your
        // adapter should download image assets on behalf of the publisher. See the
        // SampleNativeMediationEventForwarder for information on how to do so.
        request.setShouldDownloadImages(true);

        request.setShouldDownloadMultipleImages(false);
        request.setPreferredImageOrientation(SampleNativeAdRequest.IMAGE_ORIENTATION_ANY);

        NativeAdOptions options = mediationAdRequest.getNativeAdOptions();

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

        // Set App Install and Content Ad requests.
        //
        // NOTE: Care needs to be taken to make sure the adapter respects the publisher's wishes
        // in regard to native ad formats. For example, if your ad network only provides app install
        // ads, and the publisher requests content ads alone, the adapter must report an error by
        // calling the listener's onAdFailedToLoad method with an error code of
        // AdRequest.ERROR_CODE_INVALID_REQUEST. It should *not* request an app install ad anyway,
        // and then attempt to map it to the content ad format.
        if (!mediationAdRequest.isAppInstallAdRequested()
                && !mediationAdRequest.isContentAdRequested()) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        request.setAppInstallAdsRequested(mediationAdRequest.isAppInstallAdRequested());
        request.setContentAdsRequested(mediationAdRequest.isContentAdRequested());

        /**
         * If your network supports additional request parameters, the publisher can send these
         * additional parameters to the adapter using the {@link mediationExtras} bundle.
         * Creating a bundle builder class makes it easier for the publisher to create this bundle.
         */
        if (mediationExtras != null) {
            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE)) {
                request.setShouldAddAwesomeSauce(
                        mediationExtras.getBoolean(MediationExtrasBundleBuilder.KEY_AWESOME_SAUCE));
            }

            if (mediationExtras.containsKey(MediationExtrasBundleBuilder.KEY_INCOME)) {
                request.setIncome(mediationExtras.getInt(MediationExtrasBundleBuilder.KEY_INCOME));
            }
        }

        // Make an ad request.
        loader.fetchAd(request);
    }

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String unUsed,
                           MediationRewardedVideoAdListener listener,
                           Bundle serverParameters,
                           Bundle mediationExtras) {

        // In this method you should initialize your SDK.

        // The sample SDK requires activity context to initialize, so check that the context
        // provided by the app is an activity context before initializing.
        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            Log.d(TAG, "Sample SDK requires an Activity context to initialize");
            listener.onInitializationFailed(
                    SampleAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        /**
         * Get the Ad Unit ID for the Sample SDK from serverParameters bundle using the pre
         * configured keys.
         *
         * For custom events, there is a single parameter that can be accessed via
         *
         * String serverParameter = serverParameters.getString(
         *         MediationRewardedVideoAdAdapter.CUSTOM_EVENT_SERVER_PARAMETER_FIELD);
         */
        String adUnit = serverParameters.getString(SAMPLE_AD_UNIT_KEY);
        if (TextUtils.isEmpty(adUnit)) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Create a rewarded video event forwarder to forward the events from the Sample SDK to
        // the Google Mobile Ads SDK.
        mRewardedVideoEventForwarder =
                new SampleMediationRewardedVideoEventForwarder(listener, SampleAdapter.this);

        // Initialize the Sample SDK.
        SampleRewardedVideo.initialize((Activity) context, adUnit, mRewardedVideoEventForwarder);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle mediationExtras) {
        if (SampleRewardedVideo.isAdAvailable()) {
            // Ad already available, use the forwarder to send a success callback to AdMob.
            mRewardedVideoEventForwarder.onAdLoaded();
        } else {
            // No ad available, use the forwarder to send a failure callback.
            mRewardedVideoEventForwarder.onAdFailedToLoad();
        }
    }

    @Override
    public void showVideo() {
        // Show the rewarded video ad.
        if (SampleRewardedVideo.isAdAvailable()) {
            // Rewarded video ad available, show ad.
            SampleRewardedVideo.showAd();
        } else {
            // Show ad will only be called if the adapter sends back an ad loaded callback in
            // response to a loadAd request. If for any reason the adapter is not ready to show
            // an ad after sending an ad loaded callback, log a warning.
            Log.w(TAG, "No ads to show.");
        }
    }

    @Override
    public boolean isInitialized() {
        return mRewardedVideoEventForwarder != null && mRewardedVideoEventForwarder.isInitialized();
    }

    @Override
    public void onContextChanged(Context context) {
        if (context instanceof Activity) {
            SampleRewardedVideo.setCurrentActivity((Activity) context);
        }
    }

    /**
     * The {@link MediationExtrasBundleBuilder} class is used to create a mediation extras bundle
     * that can be passed to the adapter as extra data to be used in making requests. In this
     * example the sample SDK has two extra parameters that it can use to customize its ad requests.
     */
    public static final class MediationExtrasBundleBuilder {

        // Keys to add and obtain the extra parameters from the bundle.
        private static final String KEY_AWESOME_SAUCE = "awesome_sauce";
        private static final String KEY_INCOME = "income";

        /**
         * An extra value used to populate the "ShouldAddAwesomeSauce" property of the Sample
         * SDK's ad request.
         */
        private boolean mShouldAddAwesomeSauce;

        /**
         * An extra value used to populate the "income" property of the Sample SDK's ad request.
         */
        private int mIncome;

        public MediationExtrasBundleBuilder setShouldAddAwesomeSauce(
                boolean shouldAddAwesomeSauce) {
            this.mShouldAddAwesomeSauce = shouldAddAwesomeSauce;
            return MediationExtrasBundleBuilder.this;
        }

        public MediationExtrasBundleBuilder setIncome(int income) {
            this.mIncome = income;
            return MediationExtrasBundleBuilder.this;
        }

        public Bundle build() {
            Bundle extras = new Bundle();
            extras.putBoolean(KEY_AWESOME_SAUCE, mShouldAddAwesomeSauce);
            extras.putInt(KEY_INCOME, mIncome);
            return extras;
        }
    }
}
