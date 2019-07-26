// Copyright 2014 Google Inc.
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

package com.google.ads.mediation.facebook;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.MediaViewListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.ads.mediation.facebook.FacebookExtras.NATIVE_BANNER;

/**
 * Mediation adapter for Facebook Audience Network.
 */
@Keep
public final class FacebookAdapter extends FacebookMediationAdapter
        implements MediationBannerAdapter, MediationInterstitialAdapter, MediationNativeAdapter {

    public static final String KEY_ID = "id";
    public static final String KEY_SOCIAL_CONTEXT_ASSET = "social_context";

    private static final int MAX_STAR_RATING = 5;

    private MediationBannerListener mBannerListener;
    private MediationInterstitialListener mInterstitialListener;

    private MediationNativeListener mNativeListener;
    private AdView mAdView;
    private RelativeLayout mWrappedAdView;
    private InterstitialAd mInterstitialAd;
    private boolean isNativeBanner;

    /**
     * Facebook native ad instance.
     */
    private NativeAd mNativeAd;

    /**
     * Facebook native banner ad instance.
     */
    private NativeBannerAd mNativeBannerAd;

    /**
     * Flag to determine whether or not an impression callback from Facebook SDK has already been
     * sent to the Google Mobile Ads SDK.
     */
    private boolean mIsImpressionRecorded;

    /**
     * A Facebook {@link MediaView} used to show native ad media content.
     */
    private MediaView mMediaView;

    //region MediationAdapter implementation.
    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        if (mInterstitialAd != null) {
            mInterstitialAd.destroy();
        }
        if (mNativeAd != null) {
            mNativeAd.unregisterView();
            mNativeAd.destroy();
        }
        if (mMediaView != null) {
            mMediaView.destroy();
        }
        if (mNativeBannerAd != null) {
            mNativeBannerAd.unregisterView();
            mNativeBannerAd.destroy();
        }
    }

    @Override
    public void onPause() {
        // Do nothing.
    }

    @Override
    public void onResume() {
        // Do nothing.
    }
    //endregion

    //region MediationBannerAdapter implementation.
    @Override
    public void requestBannerAd(final Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                final AdSize adSize,
                                final MediationAdRequest adRequest,
                                Bundle mediationExtras) {
        mBannerListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            if (mBannerListener != null) {
                mBannerListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }

        if (adSize == null) {
            Log.w(TAG, "Fail to request banner ad, adSize is null");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        com.facebook.ads.AdSize facebookAdSize = getAdSize(context, adSize);
        if (facebookAdSize == null) {
            Log.w(TAG,
                    "The input ad size " + adSize.toString() + " is not supported at this moment.");
            mBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        final String placementId = serverParameters.getString(PLACEMENT_PARAMETER);
        FacebookInitializer.getInstance().initialize(context, placementId,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadBannerAd(context, placementId, adSize, adRequest);
            }

            @Override
            public void onInitializeError(String message) {
                Log.w(TAG, "Failed to load ad from Facebook: " + message);
                if (mBannerListener != null) {
                    mBannerListener.onAdFailedToLoad(FacebookAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
    }

    @Override
    public View getBannerView() {
        return mWrappedAdView;
    }
    //endregion

    //region MediationInterstitialAdapter implementation.
    @Override
    public void requestInterstitialAd(final Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      final MediationAdRequest adRequest,
                                      Bundle mediationExtras) {
        mInterstitialListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        final String placementId = serverParameters.getString(PLACEMENT_PARAMETER);

        FacebookInitializer.getInstance().initialize(context, placementId,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadInterstitial(context, placementId, adRequest);
            }

            @Override
            public void onInitializeError(String message) {
                Log.w(TAG, "Failed to load ad from Facebook: " + message);
                if (mInterstitialListener != null) {
                    mInterstitialListener.onAdFailedToLoad(FacebookAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialAd.isAdLoaded()) {
            mInterstitialAd.show();
        }
    }
    //endregion

    //region MediationNativeAdapter implementation.
    @Override
    public void requestNativeAd(final Context context,
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                final NativeMediationAdRequest mediationAdRequest,
                                final Bundle mediationExtras) {
        mNativeListener = listener;
        if (!isValidRequestParameters(context, serverParameters)) {
            mNativeListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        // Verify that the request is for both app install and content ads.
        if (!(mediationAdRequest.isAppInstallAdRequested()
                && mediationAdRequest.isContentAdRequested())) {
            Log.w(TAG, "Failed to request native ad. Both app install and content ad should be "
                    + "requested");
            mNativeListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        final String placementId = serverParameters.getString(PLACEMENT_PARAMETER);

        FacebookInitializer.getInstance().initialize(context, placementId,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadNativeAd(context, placementId, mediationAdRequest, mediationExtras);
            }

            @Override
            public void onInitializeError(String message) {
                Log.w(TAG, "Failed to load ad from Facebook: " + message);
                if (mNativeListener != null) {
                    mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
    }
    //endregion

    //region Common methods.
    /**
     * Converts an {@link AdError} code to Google Mobile Ads SDK readable error code.
     *
     * @param adError the {@link AdError} to be converted.
     * @return an {@link AdRequest} error code.
     */
    private int convertErrorCode(AdError adError) {
        if (adError == null) {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
        int errorCode = adError.getErrorCode();
        switch (errorCode) {
            case AdError.NETWORK_ERROR_CODE:
            case AdError.SERVER_ERROR_CODE:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case AdError.NO_FILL_ERROR_CODE:
                return AdRequest.ERROR_CODE_NO_FILL;
            case AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case AdError.INTERNAL_ERROR_CODE:
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }

    private void buildAdRequest(MediationAdRequest adRequest) {
        if (adRequest != null) {
            AdSettings.setIsChildDirected((adRequest.taggedForChildDirectedTreatment()
                    == MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE));
        }
    }
    //endregion

    //region Banner adapter utility classes.
    private void createAndLoadBannerAd(Context context,
                                       String placementID,
                                       AdSize adSize,
                                       MediationAdRequest adRequest) {
        com.facebook.ads.AdSize facebookAdSize = getAdSize(context, adSize);

        mAdView = new AdView(context, placementID, facebookAdSize);
        mAdView.setAdListener(new BannerListener());
        buildAdRequest(adRequest);

        RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
                adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
        mWrappedAdView = new RelativeLayout(context);
        mAdView.setLayoutParams(adViewLayoutParams);
        mWrappedAdView.addView(mAdView);
        mAdView.loadAd();
    }

    private class BannerListener implements AdListener {
        private BannerListener() {
        }

        @Override
        public void onAdClicked(Ad ad) {
            FacebookAdapter.this.mBannerListener.onAdClicked(FacebookAdapter.this);
            FacebookAdapter.this.mBannerListener.onAdOpened(FacebookAdapter.this);
            // The test Facebook ads leave the application when the ad is clicked. Assuming all
            // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
            FacebookAdapter.this.mBannerListener.onAdLeftApplication(FacebookAdapter.this);
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            // Google Mobile Ads SDK does its own impression tracking for banner ads.
        }

        @Override
        public void onAdLoaded(Ad ad) {
            FacebookAdapter.this.mBannerListener.onAdLoaded(FacebookAdapter.this);
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mBannerListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }
    }
    //endregion

    //region Interstitial adapter utility classes.
    private void createAndLoadInterstitial(Context context,
                                           String placementID,
                                           MediationAdRequest adRequest) {
        mInterstitialAd = new InterstitialAd(context, placementID);
        mInterstitialAd.setAdListener(new InterstitialListener());
        buildAdRequest(adRequest);
        mInterstitialAd.loadAd();
    }

    private class InterstitialListener implements InterstitialAdListener {
        private InterstitialListener() {
        }

        @Override
        public void onAdClicked(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdClicked(FacebookAdapter.this);
            // The test Facebook ads leave the application when the ad is clicked. Assuming all
            // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
            FacebookAdapter.this.mInterstitialListener.onAdLeftApplication(FacebookAdapter.this);
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            // Google Mobile Ads SDK does its own impression tracking for interstitial ads.
        }

        @Override
        public void onAdLoaded(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdLoaded(FacebookAdapter.this);
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mInterstitialListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }

        @Override
        public void onInterstitialDismissed(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdClosed(FacebookAdapter.this);
        }

        @Override
        public void onInterstitialDisplayed(Ad ad) {
            FacebookAdapter.this.mInterstitialListener.onAdOpened(FacebookAdapter.this);
        }
    }
    //endregion

    //region Native adapter utility methods and classes.
    private void createAndLoadNativeAd(Context context,
                                       String placementID,
                                       NativeMediationAdRequest adRequest,
                                       Bundle mediationExtras) {

        if (mediationExtras != null) {
            isNativeBanner = mediationExtras.getBoolean(NATIVE_BANNER);
        }
        if (isNativeBanner) {
            mNativeBannerAd = new NativeBannerAd(context, placementID);
            mNativeBannerAd.setAdListener(new NativeBannerListener(mNativeBannerAd, adRequest));
            buildAdRequest(adRequest);
            mNativeBannerAd.loadAd();
        } else {
            mMediaView = new MediaView(context);
            mNativeAd = new NativeAd(context, placementID);
            mNativeAd.setAdListener(new NativeListener(mNativeAd, adRequest));
            buildAdRequest(adRequest);
            mNativeAd.loadAd();
        }
    }

    private class NativeBannerListener implements  AdListener, NativeAdListener {

        private NativeBannerAd mNativeBannerAd;
        private NativeMediationAdRequest mMediationAdRequest;

        private NativeBannerListener(NativeBannerAd nativeBannerAd,
                                     NativeMediationAdRequest mediationAdRequest) {
            mNativeBannerAd = nativeBannerAd;
            mMediationAdRequest = mediationAdRequest;
        }

        @Override
        public void onMediaDownloaded(Ad ad) {
            Log.d(TAG, "onMediaDownloaded");

        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));

        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (ad!= mNativeBannerAd)  {
                Log.w(TAG, "Ad loaded is not a native banner ad.");
                FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                return;
            }

            NativeAdOptions options = mMediationAdRequest.getNativeAdOptions();
            if (mMediationAdRequest.isUnifiedNativeAdRequested()) {
                final UnifiedAdMapper mapper;
                mapper = new UnifiedAdMapper(mNativeBannerAd, options);
                mapper.mapUnifiedNativeAd(new NativeAdMapperListener() {
                    @Override
                    public void onMappingSuccess() {
                        mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
                        Log.e("INFO","On Ad Loaded");
                    }

                    @Override
                    public void onMappingFailed() {
                        mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                });

            } else if (mMediationAdRequest.isAppInstallAdRequested()) {
                // We always convert the ad into an app install ad.
                final AppInstallMapper mapper = new AppInstallMapper(mNativeBannerAd, options);
                mapper.mapNativeAd(new NativeAdMapperListener() {
                    @Override
                    public void onMappingSuccess() {
                        mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
                    }

                    @Override
                    public void onMappingFailed() {
                        mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                });
            } else {
                Log.e(TAG, "Content Ads are not supported.");
                FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

        }

        @Override
        public void onAdClicked(Ad ad) {
            FacebookAdapter.this.mNativeListener.onAdClicked(FacebookAdapter.this);
            FacebookAdapter.this.mNativeListener.onAdOpened(FacebookAdapter.this);
            // The test Facebook ads leave the application when the ad is clicked. Assuming all
            // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
            FacebookAdapter.this.mNativeListener.onAdLeftApplication(FacebookAdapter.this);

        }

        @Override
        public void onLoggingImpression(Ad ad) {
            if (mIsImpressionRecorded) {
                Log.d(TAG, "Received onLoggingImpression callback for a native whose impression"
                        + " is already recorded. Ignoring the duplicate callback.");
                return;
            }
            FacebookAdapter.this.mNativeListener.onAdImpression(FacebookAdapter.this);
            mIsImpressionRecorded = true;

        }
    }

    private class NativeListener implements AdListener, NativeAdListener {
        private NativeAd mNativeAd;
        private NativeMediationAdRequest mMediationAdRequest;

        private NativeListener(NativeAd nativeAd, NativeMediationAdRequest mediationAdRequest) {
            mNativeAd = nativeAd;
            mMediationAdRequest = mediationAdRequest;
        }

        @Override
        public void onAdClicked(Ad ad) {
            FacebookAdapter.this.mNativeListener.onAdClicked(FacebookAdapter.this);
            FacebookAdapter.this.mNativeListener.onAdOpened(FacebookAdapter.this);
            // The test Facebook ads leave the application when the ad is clicked. Assuming all
            // the ads do the same, sending onAdLeftApplication callback when the ad is clicked.
            FacebookAdapter.this.mNativeListener.onAdLeftApplication(FacebookAdapter.this);
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            if (mIsImpressionRecorded) {
                Log.d(TAG, "Received onLoggingImpression callback for a native whose impression"
                        + " is already recorded. Ignoring the duplicate callback.");
                return;
            }
            FacebookAdapter.this.mNativeListener.onAdImpression(FacebookAdapter.this);
            mIsImpressionRecorded = true;
        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (ad != mNativeAd) {
                Log.w(TAG, "Ad loaded is not a native ad.");
                FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                return;
            }

            NativeAdOptions options = mMediationAdRequest.getNativeAdOptions();
            if (mMediationAdRequest.isUnifiedNativeAdRequested()) {
                final UnifiedAdMapper mapper = new UnifiedAdMapper(mNativeAd, options);
                mapper.mapUnifiedNativeAd(new NativeAdMapperListener() {
                    @Override
                    public void onMappingSuccess() {
                        mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
                    }

                    @Override
                    public void onMappingFailed() {
                        mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                });

            } else if (mMediationAdRequest.isAppInstallAdRequested()) {
                // We always convert the ad into an app install ad.
                final AppInstallMapper mapper = new AppInstallMapper(mNativeAd, options);
                mapper.mapNativeAd(new NativeAdMapperListener() {
                    @Override
                    public void onMappingSuccess() {
                        mNativeListener.onAdLoaded(FacebookAdapter.this, mapper);
                    }

                    @Override
                    public void onMappingFailed() {
                        mNativeListener.onAdFailedToLoad(FacebookAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                });
            } else {
                Log.e(TAG, "Content Ads are not supported.");
                FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                        FacebookAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, errorMessage);
            }
            FacebookAdapter.this.mNativeListener.onAdFailedToLoad(
                    FacebookAdapter.this, convertErrorCode(adError));
        }

        @Override
        public void onMediaDownloaded(Ad ad) {
            Log.d(TAG, "onMediaDownloaded");
        }
    }

    private com.facebook.ads.AdSize getAdSize(Context context, AdSize adSize) {

        // Get the actual width of the ad size since Smart Banners and FULL_WIDTH sizes return a
        // width of -1.
        int width = adSize.getWidth();
        if (width < 0) {
            float density = context.getResources().getDisplayMetrics().density;
            width = Math.round(adSize.getWidthInPixels(context) / density);
        }

        ArrayList<AdSize> potentials = new ArrayList<>(3);
        potentials.add(0, new AdSize(width, 50));
        potentials.add(1, new AdSize(width, 90));
        potentials.add(2, new AdSize(width, 250));
        Log.i(TAG, "Potential ad sizes: " + potentials.toString());
        AdSize closestSize = findClosestSize(context, adSize, potentials);
        if (closestSize == null) {
            return null;
        }
        Log.i(TAG, "Found closest ad size: " + closestSize.toString());

        int adHeight = closestSize.getHeight();
        if (adHeight == com.facebook.ads.AdSize.BANNER_HEIGHT_50.getHeight()) {
            return com.facebook.ads.AdSize.BANNER_HEIGHT_50;
        }

        if (adHeight == com.facebook.ads.AdSize.BANNER_HEIGHT_90.getHeight()) {
            return com.facebook.ads.AdSize.BANNER_HEIGHT_90;
        }

        if (adHeight == com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return com.facebook.ads.AdSize.RECTANGLE_HEIGHT_250;
        }
        return null;
    }

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size. Returns
     * null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
        original = new AdSize(actualWidth, actualHeight);

        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    private static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK

    /**
     * The {@link AppInstallMapper} class is used to map Facebook native ads to Google Mobile Ads'
     * native app install ads.
     */
    class AppInstallMapper extends NativeAppInstallAdMapper {
        /**
         * The Facebook native ad to be mapped.
         */
        private NativeAd mNativeAd;

        /**
         * The Facebook native banner ad to be mapped.
         */
        private NativeBannerAd mNativeBannerAd;

        /**
         * Google Mobile Ads native ad options.
         */
        private NativeAdOptions mNativeAdOptions;

        /**
         * Default constructor for {@link AppInstallMapper}.
         *
         * @param nativeAd  The Facebook native ad to be mapped.
         * @param adOptions {@link NativeAdOptions} containing the preferences to be used when
         *                  mapping the native ad.
         */
        public AppInstallMapper(NativeAd nativeAd, NativeAdOptions adOptions) {
            AppInstallMapper.this.mNativeAd = nativeAd;
            AppInstallMapper.this.mNativeAdOptions = adOptions;
        }

        /**
         * Constructor for {@link AppInstallMapper}.
         *
         * @param nativeBannerAd The Facebook native banner ad to be mapped.
         * @param adOptions {@link NativeAdOptions} containing the preferences to be used when
         *                  mapping the native ad.
         */
        public AppInstallMapper(NativeBannerAd nativeBannerAd, NativeAdOptions adOptions) {
            AppInstallMapper.this.mNativeBannerAd = nativeBannerAd;
            AppInstallMapper.this.mNativeAdOptions = adOptions;
        }

        /**
         * This method will map the Facebook {@link #mNativeAd} to this mapper and send a success
         * callback if the mapping was successful or a failure callback if the mapping was
         * unsuccessful.
         *
         * @param mapperListener used to send success/failure callbacks when mapping is done.
         */
        public void mapNativeAd(NativeAdMapperListener mapperListener) {
            if (isNativeBanner) {
                if (!containsRequiredFieldsForNativeBannerAd(mNativeBannerAd)) {
                    Log.w(TAG, "Ad from Facebook doesn't have all assets required for the Native " +
                            "Banner Ad"
                            + " format.");
                    mapperListener.onMappingFailed();
                    return;
                }

                setHeadline(mNativeBannerAd.getAdHeadline());
                setBody(mNativeBannerAd.getAdBodyText());
                setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeBannerAd.getAdIcon().toString())));
                setCallToAction(mNativeBannerAd.getAdCallToAction());
                Bundle extras = new Bundle();
                extras.putCharSequence(KEY_ID, mNativeBannerAd.getId());
                extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET,
                        mNativeBannerAd.getAdSocialContext());
                setExtras(extras);
            } else {
                if (!containsRequiredFieldsForNativeAppInstallAd(mNativeAd)) {
                    Log.w(TAG, "Ad from Facebook doesn't have all assets required for the app install"
                            + " format.");
                    mapperListener.onMappingFailed();
                    return;
                }

                // Map all required assets (headline, one image, body, icon and call to
                // action).
                setHeadline(mNativeAd.getAdHeadline());
                List<com.google.android.gms.ads.formats.NativeAd.Image> images = new ArrayList<>();
                images.add(new FacebookAdapterNativeAdImage(
                        Uri.parse(mNativeAd.getAdCoverImage().toString())));
                setImages(images);
                setBody(mNativeAd.getAdBodyText());
                setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeAd.getAdIcon().toString())));
                setCallToAction(mNativeAd.getAdCallToAction());

                mMediaView.setListener(new MediaViewListener() {
                    @Override
                    public void onPlay(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVolumeChange(MediaView mediaView, float v) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onPause(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onComplete(MediaView mediaView) {
                        if (FacebookAdapter.this.mNativeListener != null) {
                            FacebookAdapter.this.mNativeListener.onVideoEnd(FacebookAdapter.this);
                        }
                    }

                    @Override
                    public void onEnterFullscreen(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onExitFullscreen(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onFullscreenBackground(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onFullscreenForeground(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }
                });

                // Because the FAN SDK doesn't offer a way to determine whether a native ad contains
                // a video asset or not, the adapter always returns a MediaView and claims to have
                // video content.
                setMediaView(mMediaView);
                setHasVideoContent(true);

                // Map the optional assets.
                Double starRating = getRating(mNativeAd.getAdStarRating());
                if (starRating != null) {
                    setStarRating(starRating);
                }


                // Pass all the assets not supported by Google as extras.
                Bundle extras = new Bundle();
                extras.putCharSequence(KEY_ID, mNativeAd.getId());
                extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET, mNativeAd.getAdSocialContext());
                setExtras(extras);
            }

            mapperListener.onMappingSuccess();
        }

        /**
         * This method will check whether or not the given Facebook native ad contains all the
         * necessary fields for it to be mapped to Google Mobile Ads' native app install ad.
         *
         * @param nativeAd Facebook native ad.
         * @return {@code true} if the given ad contains all the necessary fields, {@link false}
         * otherwise.
         */
        private boolean containsRequiredFieldsForNativeAppInstallAd(NativeAd nativeAd) {
            return ((nativeAd.getAdHeadline() != null) && (nativeAd.getAdCoverImage() != null)
                    && (nativeAd.getAdBodyText() != null) && (nativeAd.getAdIcon() != null)
                    && (nativeAd.getAdCallToAction() != null) && (mMediaView != null));
        }

        /**
         * This method will check whether or not the given Facebook native banner ad contains all the
         * necessary fields for it to be mapped to Google Mobile Ads' native app install ad.
         *
         * @param nativeBannerAd Facebook native banner ad.
         * @return {@code true} if the given ad contains all the necessary fields, {@link false}
         * otherwise.
         */
        private boolean containsRequiredFieldsForNativeBannerAd(NativeBannerAd nativeBannerAd) {
            return ((nativeBannerAd.getAdHeadline() != null)
                    && (nativeBannerAd.getAdBodyText() != null) && (nativeBannerAd.getAdIcon() != null)
                    && (nativeBannerAd.getAdCallToAction() != null));
        }

        @Override
        public void trackViews(View view,
                               Map<String, View> clickableAssetViews,
                               Map<String, View> nonClickableAssetViews) {

            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            NativeAdLayout nativeAdLayout = new NativeAdLayout(view.getContext());
            if (overlayView instanceof FrameLayout) {
                ((FrameLayout) overlayView).addView(nativeAdLayout);
                // Create and add Facebook's AdOptions to the overlay view.
                AdOptionsView adOptionsView;
                if (isNativeBanner) {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeBannerAd,
                            nativeAdLayout);
                } else {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeAd, nativeAdLayout);
                }
                ((ViewGroup) overlayView).addView(adOptionsView);
                // We know that the overlay view is a FrameLayout, so we get the FrameLayout's
                // LayoutParams from the AdOptionsView.
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) adOptionsView.getLayoutParams();
                if (mNativeAdOptions != null) {
                    switch (mNativeAdOptions.getAdChoicesPlacement()) {
                        case NativeAdOptions.ADCHOICES_TOP_LEFT:
                            params.gravity = Gravity.TOP | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                            params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                            params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                        default:
                            params.gravity = Gravity.TOP | Gravity.RIGHT;
                    }
                } else {
                    // Default to top right if native ad options are not provided.
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                }
                adView.requestLayout();
            } else {
                AdOptionsView adOptionsView;
                if (isNativeBanner) {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeBannerAd,
                            nativeAdLayout);
                } else {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeAd, nativeAdLayout);
                }
                this.setAdChoicesContent(adOptionsView);
            }

            // Facebook does its own impression tracking.
            setOverrideImpressionRecording(true);

            // Facebook does its own click handling.
            setOverrideClickHandling(true);
            ImageView iconview = null;

            ArrayList<View> assetViews = new ArrayList<>();
            for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
                assetViews.add(clickableAssets.getValue());

                if (clickableAssets.getKey().equals(NativeAppInstallAd.ASSET_ICON) ||
                        clickableAssets.getKey().equals(UnifiedNativeAdAssetNames.ASSET_ICON)) {
                    iconview = (ImageView) clickableAssets.getValue();
                }

            }
            if (isNativeBanner) {
                mNativeBannerAd.registerViewForInteraction(view, iconview);
            } else {
                mNativeAd.registerViewForInteraction(view, mMediaView, iconview, assetViews);
            }
        }


        @Override
        public void untrackView(View view) {
            super.untrackView(view);
            // Called when the native ad view no longer needs tracking. Remove any previously
            // added trackers.

            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            if (overlayView instanceof FrameLayout) {
                ((FrameLayout) overlayView).removeAllViews();
            }

            if (isNativeBanner) {
                mNativeAd.unregisterView();
            } else {
                mNativeBannerAd.unregisterView();
            }
        }

        /**
         * Convert rating to a scale of 1 to 5.
         */
        private Double getRating(NativeAd.Rating rating) {
            if (rating == null) {
                return null;
            }
            return (MAX_STAR_RATING * rating.getValue()) / rating.getScale();
        }
    }

    /**
     * The {@link UnifiedAdMapper} class is used to map Facebook native ads to Google unified native ads.
     */
    class UnifiedAdMapper extends UnifiedNativeAdMapper {

        /**
         * The Facebook native ad to be mapped.
         */
        private NativeAd mNativeAd;

        /**
         * The Facebook native banner ad to be mapped.
         */
        private NativeBannerAd mNativeBannerAd;

        /**
         * Google Mobile Ads native ad options.
         */
        private NativeAdOptions mNativeAdOptions;

        /**
         * Default constructor for {@link UnifiedAdMapper}.
         *
         * @param nativeAd  The Facebook native ad to be mapped.
         * @param adOptions {@link NativeAdOptions} containing the preferences to be used when
         *                  mapping the native ad.
         */
        public UnifiedAdMapper(NativeAd nativeAd, NativeAdOptions adOptions) {
            UnifiedAdMapper.this.mNativeAd = nativeAd;
            UnifiedAdMapper.this.mNativeAdOptions = adOptions;
        }

        /**
         * Constructor for {@link UnifiedAdMapper}.
         *
         * @param nativeBannerAd  The Facebook native banner ad to be mapped.
         * @param adOptions {@link NativeAdOptions} containing the preferences to be used when
         *                  mapping the native ad.
         */
        public UnifiedAdMapper(NativeBannerAd nativeBannerAd, NativeAdOptions adOptions){
            UnifiedAdMapper.this.mNativeBannerAd = nativeBannerAd;
            UnifiedAdMapper.this.mNativeAdOptions = adOptions;
        }

        /**
         * This method will map the Facebook {@link #mNativeAd} to this mapper and send a success
         * callback if the mapping was successful or a failure callback if the mapping was
         * unsuccessful.
         *
         * @param mapperListener used to send success/failure callbacks when mapping is done.
         */
        public void mapUnifiedNativeAd(NativeAdMapperListener mapperListener) {

            if (isNativeBanner) {
                if (!containsRequiredFieldsForNativeBannerAd(mNativeBannerAd)) {
                    Log.w(TAG, "Ad from Facebook doesn't have all assets required for the " +
                            "Native Banner Ad format.");
                    mapperListener.onMappingFailed();
                    return;
                }

                setHeadline(mNativeBannerAd.getAdHeadline());
                setBody(mNativeBannerAd.getAdBodyText());
                setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeBannerAd.getAdIcon().toString())));
                setCallToAction(mNativeBannerAd.getAdCallToAction());
                setAdvertiser(mNativeBannerAd.getAdvertiserName());

                Bundle extras = new Bundle();
                extras.putCharSequence(KEY_ID, mNativeBannerAd.getId());
                extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET,
                        mNativeBannerAd.getAdSocialContext());
                setExtras(extras);
            } else {
                if (!containsRequiredFieldsForUnifiedNativeAd(mNativeAd)) {
                    Log.w(TAG, "Ad from Facebook doesn't have all assets required for the" +
                            " Native Ad format.");
                    mapperListener.onMappingFailed();
                    return;
                }
                // Map all required assets (headline, one image, body, icon and call to
                // action).
                setHeadline(mNativeAd.getAdHeadline());
                List<com.google.android.gms.ads.formats.NativeAd.Image> images = new ArrayList<>();
                images.add(new FacebookAdapterNativeAdImage(
                        Uri.parse(mNativeAd.getAdCoverImage().toString())));
                setImages(images);
                setBody(mNativeAd.getAdBodyText());
                setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeAd.getAdIcon().toString())));
                setCallToAction(mNativeAd.getAdCallToAction());
                setAdvertiser(mNativeAd.getAdvertiserName());

                mMediaView.setListener(new MediaViewListener() {
                    @Override
                    public void onPlay(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVolumeChange(MediaView mediaView, float v) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onPause(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onComplete(MediaView mediaView) {
                        if (FacebookAdapter.this.mNativeListener != null) {
                            FacebookAdapter.this.mNativeListener.onVideoEnd(FacebookAdapter.this);
                        }
                    }

                    @Override
                    public void onEnterFullscreen(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onExitFullscreen(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onFullscreenBackground(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onFullscreenForeground(MediaView mediaView) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }
                });

                // Because the FAN SDK doesn't offer a way to determine whether a native ad contains
                // a video asset or not, the adapter always returns a MediaView and claims to have
                // video content.
                setMediaView(mMediaView);
                setHasVideoContent(true);

                // Map the optional assets.
                Double starRating = getRating(mNativeAd.getAdStarRating());
                if (starRating != null) {
                    setStarRating(starRating);
                }
                Bundle extras = new Bundle();
                extras.putCharSequence(KEY_ID, mNativeAd.getId());
                extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET, mNativeAd.getAdSocialContext());
                setExtras(extras);
            }
            mapperListener.onMappingSuccess();
        }

        /**
         * This method will check whether or not the given Facebook native ad contains all the
         * necessary fields for it to be mapped to Google Mobile Ads' Unified install ad.
         *
         * @param nativeAd Facebook native ad.
         * @return {@code true} if the given ad contains all the necessary fields, {@link false}
         * otherwise.
         */
        private boolean containsRequiredFieldsForUnifiedNativeAd(NativeAd nativeAd) {
            return ((nativeAd.getAdHeadline() != null) && (nativeAd.getAdCoverImage() != null)
                    && (nativeAd.getAdBodyText() != null) && (nativeAd.getAdIcon() != null)
                    && (nativeAd.getAdCallToAction() != null) && (mMediaView != null));
        }

        /**
         * This method will check whether or not the given Facebook native ad contains all the
         * necessary fields for it to be mapped to Google Mobile Ads' Unified install ad.
         *
         * @param nativeBannerAd Facebook native ad.
         * @return {@code true} if the given ad contains all the necessary fields, {@link false}
         * otherwise.
         */
        private boolean containsRequiredFieldsForNativeBannerAd(NativeBannerAd nativeBannerAd) {
            return ((nativeBannerAd.getAdHeadline() != null) &&
                    (nativeBannerAd.getAdBodyText() != null) && (nativeBannerAd.getAdIcon() != null)
                    && (nativeBannerAd.getAdCallToAction() != null));
        }


        @Override
        public void trackViews(View view,
                               Map<String, View> clickableAssetViews,
                               Map<String, View> nonClickableAssetViews) {

            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            NativeAdLayout nativeAdLayout = new NativeAdLayout(view.getContext());
            if (overlayView instanceof FrameLayout) {
                AdOptionsView adOptionsView;
                // Create and add Facebook's AdOptions to the overlay view.
                if (isNativeBanner) {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeBannerAd,
                            nativeAdLayout);
                } else {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeAd, nativeAdLayout);
                }
                ((ViewGroup) overlayView).addView(adOptionsView);
                // We know that the overlay view is a FrameLayout, so we get the FrameLayout's
                // LayoutParams from the AdOptionsView.
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) adOptionsView.getLayoutParams();
                if (mNativeAdOptions != null) {
                    switch (mNativeAdOptions.getAdChoicesPlacement()) {
                        case NativeAdOptions.ADCHOICES_TOP_LEFT:
                            params.gravity = Gravity.TOP | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                            params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                            params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                            break;
                        case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                        default:
                            params.gravity = Gravity.TOP | Gravity.RIGHT;
                    }
                } else {
                    // Default to top right if native ad options are not provided.
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                }
                adView.requestLayout();
            } else {
                AdOptionsView adOptionsView;
                if (isNativeBanner) {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeBannerAd,
                            nativeAdLayout);
                } else {
                    adOptionsView = new AdOptionsView(view.getContext(), mNativeAd, nativeAdLayout);
                }
                this.setAdChoicesContent(adOptionsView);
            }

            // Facebook does its own impression tracking.
            setOverrideImpressionRecording(true);

            // Facebook does its own click handling.
            setOverrideClickHandling(true);
            ImageView iconview = null;

            ArrayList<View> assetViews = new ArrayList<>();
            for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
                assetViews.add(clickableAssets.getValue());

                if (clickableAssets.getKey().equals(NativeAppInstallAd.ASSET_ICON) ||
                        clickableAssets.getKey().equals(UnifiedNativeAdAssetNames.ASSET_ICON)) {
                    iconview = (ImageView) clickableAssets.getValue();
                }
            }
            if (isNativeBanner) {
                mNativeBannerAd.registerViewForInteraction(view, iconview);
            } else {
                mNativeAd.registerViewForInteraction(view, mMediaView, iconview, assetViews);
            }
        }


        @Override
        public void untrackView(View view) {
            super.untrackView(view);
            // Called when the native ad view no longer needs tracking. Remove any previously
            // added trackers.

            ViewGroup adView = (ViewGroup) view;
            // Find the overlay view in the given ad view. The overlay view will always be the
            // top most view in the hierarchy.
            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            if (overlayView instanceof FrameLayout) {
                ((FrameLayout) overlayView).removeAllViews();
            }
            if (isNativeBanner) {
                mNativeAd.unregisterView();
            } else {
                mNativeBannerAd.unregisterView();
            }
        }

        /**
         * Convert rating to a scale of 1 to 5.
         */
        private Double getRating(NativeAd.Rating rating) {
            if (rating == null) {
                return null;
            }
            return (MAX_STAR_RATING * rating.getValue()) / rating.getScale();
        }
    }

    /**
     * The {@link FacebookAdapterNativeAdImage} class is a subclass of
     * {@link com.google.android.gms.ads.formats.NativeAd.Image} used by the {@link FacebookAdapter}
     * to create images for native ads.
     */
    private class FacebookAdapterNativeAdImage extends
            com.google.android.gms.ads.formats.NativeAd.Image {

        /**
         * A drawable for the Image.
         */
        private Drawable mDrawable;

        /**
         * An Uri from which the image can be obtained.
         */
        private Uri mUri;

        /**
         * Default constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Uri}.
         *
         * @param uri required to initialize.
         */
        public FacebookAdapterNativeAdImage(Uri uri) {
            this.mUri = uri;
        }

        /**
         * @param drawable set to {@link #mDrawable}.
         */
        protected void setDrawable(Drawable drawable) {
            this.mDrawable = drawable;
        }

        @Override
        public Drawable getDrawable() {
            return mDrawable;
        }

        @Override
        public Uri getUri() {
            return mUri;
        }

        @Override
        public double getScale() {
            // Default scale is 1.
            return 1;
        }
    }

    /**
     * The {@link NativeAdMapperListener} interface is used to notify the success/failure
     * events after trying to map the native ad.
     */
    private interface NativeAdMapperListener {

        /**
         * This method will be called once the native ad mapping is successfully.
         */
        void onMappingSuccess();

        /**
         * This method will be called if the native ad mapping failed.
         */
        void onMappingFailed();
    }
    //endregion
}
