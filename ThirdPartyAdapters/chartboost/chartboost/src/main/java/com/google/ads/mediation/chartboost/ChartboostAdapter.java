// Copyright 2016 Google Inc.
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

package com.google.ads.mediation.chartboost;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.util.Log;

import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost.CBFramework;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

/**
 * The {@link ChartboostAdapter} class is used to load Chartboost rewarded-based video &
 * interstitial ads and mediate the callbacks between Chartboost SDK and Google Mobile Ads SDK.
 */
@Keep
public class ChartboostAdapter implements MediationRewardedVideoAdAdapter,
        MediationInterstitialAdapter {
    protected static final String TAG = ChartboostAdapter.class.getSimpleName();

    /**
     * The current version of the adapter.
     */
    public static final String ADAPTER_VERSION_NAME = "7.3.1.0";

    /**
     * Key to obtain App ID, required for initializing Chartboost SDK.
     */
    private static final String KEY_APP_ID = "appId";

    /**
     * Key to obtain App Signature, required for initializing Charboost SDK.
     */
    private static final String KEY_APP_SIGNATURE = "appSignature";

    /**
     * Key to obtain Ad Location. This is added in adapter version 1.1.0.
     */
    private static final String KEY_AD_LOCATION = "adLocation";

    /**
     * Flag to keep track of whether or not this {@link ChartboostAdapter} is initialized.
     */
    private boolean mIsInitialized;

    /**
     * Flag to keep track of whether or not this {@link ChartboostAdapter} is loading ads.
     */
    private boolean mIsLoading;

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * Chartboost SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    /**
     * Mediation interstitial listener used to forward interstitial ad events from Chartboost SDK
     * to Google Mobile Ads SDK.
     */
    private MediationInterstitialListener mMediationInterstitialListener;

    /**
     * A Chartboost extras object used to store optional information used when loading ads.
     */
    private ChartboostParams mChartboostParams = new ChartboostParams();

    /**
     * The Abstract Chartboost adapter delegate used to forward events received from
     * {@link ChartboostSingleton} to Google Mobile Ads SDK for interstitial ads.
     */
    private AbstractChartboostAdapterDelegate mChartboostInterstitialDelegate =
            new AbstractChartboostAdapterDelegate() {

                @Override
                public ChartboostParams getChartboostParams() {
                    return mChartboostParams;
                }

                @Override
                public void didCacheInterstitial(String location) {
                    super.didCacheInterstitial(location);
                    if (mMediationInterstitialListener != null && mIsLoading
                            && location.equals(mChartboostParams.getLocation())) {
                        mMediationInterstitialListener.onAdLoaded(ChartboostAdapter.this);
                        mIsLoading = false;
                    }
                }

                @Override
                public void didFailToLoadInterstitial(String location,
                                                      CBError.CBImpressionError error) {
                    super.didFailToLoadInterstitial(location, error);
                    if (mMediationInterstitialListener != null
                            && location.equals(mChartboostParams.getLocation())) {
                        if (mIsLoading) {
                            mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this,
                                    getAdRequestErrorType(error));
                            mIsLoading = false;
                        } else if (error
                                == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
                            // Chartboost sends the CBErrorInternetUnavailableAtShow error when
                            // the Chartboost SDK fails to show an ad because no network connection
                            // is available.
                            mMediationInterstitialListener.onAdOpened(ChartboostAdapter.this);
                            mMediationInterstitialListener.onAdClosed(ChartboostAdapter.this);
                        }
                    }
                }

                @Override
                public void didDismissInterstitial(String location) {
                    super.didDismissInterstitial(location);
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdClosed(ChartboostAdapter.this);
                    }
                }

                @Override
                public void didClickInterstitial(String location) {
                    super.didClickInterstitial(location);
                    if (mMediationInterstitialListener != null) {
                        // Chartboost doesn't have a delegate method for when an ad left
                        // application. Assuming that when an interstitial ad is clicked and the
                        // user is taken out of the application to show a web page, we forward
                        // the ad left application event to Google Mobile Ads SDK.
                        mMediationInterstitialListener.onAdClicked(ChartboostAdapter.this);
                        mMediationInterstitialListener.onAdLeftApplication(ChartboostAdapter.this);
                    }
                }

                @Override
                public void didDisplayInterstitial(String location) {
                    super.didDisplayInterstitial(location);
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdOpened(ChartboostAdapter.this);
                    }
                }
            };

    /**
     * The Abstract Chartboost adapter delegate used to forward events received from
     * {@link ChartboostSingleton} to Google Mobile Ads SDK for rewarded video ads.
     */
    private AbstractChartboostAdapterDelegate mChartboostRewardedVideoDelegate =
            new AbstractChartboostAdapterDelegate() {

                @Override
                public ChartboostParams getChartboostParams() {
                    return mChartboostParams;
                }

                @Override
                public void didInitialize() {
                    super.didInitialize();
                    if (mMediationRewardedVideoAdListener != null && !mIsInitialized) {
                        mIsInitialized = true;
                        mMediationRewardedVideoAdListener.onInitializationSucceeded(
                                ChartboostAdapter.this);
                    }
                }

                @Override
                public void didCacheRewardedVideo(String location) {
                    super.didCacheRewardedVideo(location);
                    if (mMediationRewardedVideoAdListener != null && mIsLoading
                            && location.equals(mChartboostParams.getLocation())) {
                        mMediationRewardedVideoAdListener.onAdLoaded(ChartboostAdapter.this);
                        mIsLoading = false;
                    }
                }

                @Override
                public void didFailToLoadRewardedVideo(String location,
                                                       CBError.CBImpressionError error) {
                    super.didFailToLoadRewardedVideo(location, error);
                    if (mMediationRewardedVideoAdListener != null
                            && location.equals(mChartboostParams.getLocation())) {
                        if (mIsLoading) {
                            mMediationRewardedVideoAdListener.onAdFailedToLoad(
                                    ChartboostAdapter.this, getAdRequestErrorType(error));
                            mIsLoading = false;
                        } else if (error
                                == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
                            // Chartboost sends the CBErrorInternetUnavailableAtShow error when
                            // the Chartboost SDK fails to show an ad because no network connection
                            // is available.
                            mMediationRewardedVideoAdListener.onAdOpened(ChartboostAdapter.this);
                            mMediationRewardedVideoAdListener.onAdClosed(ChartboostAdapter.this);
                        }
                    }
                }

                @Override
                public void didDismissRewardedVideo(String location) {
                    super.didDismissRewardedVideo(location);
                    if (mMediationRewardedVideoAdListener != null) {
                        mMediationRewardedVideoAdListener.onAdClosed(ChartboostAdapter.this);
                    }
                }

                @Override
                public void didClickRewardedVideo(String location) {
                    super.didClickRewardedVideo(location);
                    if (mMediationRewardedVideoAdListener != null) {
                        // Chartboost doesn't have an on ad left application callback. We assume
                        // that when an ad is clicked the user is taken out of the app.
                        mMediationRewardedVideoAdListener.onAdClicked(ChartboostAdapter.this);
                        mMediationRewardedVideoAdListener.onAdLeftApplication(
                                ChartboostAdapter.this);
                    }
                }

                @Override
                public void didCompleteRewardedVideo(String location, int reward) {
                    super.didCompleteRewardedVideo(location, reward);
                    if (mMediationRewardedVideoAdListener != null) {
                        mMediationRewardedVideoAdListener.onVideoCompleted(ChartboostAdapter.this);
                        mMediationRewardedVideoAdListener.onRewarded(ChartboostAdapter.this,
                                new ChartboostReward(reward));
                    }
                }

                @Override
                public void didDisplayRewardedVideo(String location) {
                    super.didDisplayRewardedVideo(location);
                    if (mMediationRewardedVideoAdListener != null) {
                        // Charboost doesn't have a video started callback. We assume that the video
                        // started once the ad has been displayed.
                        mMediationRewardedVideoAdListener.onAdOpened(ChartboostAdapter.this);
                        mMediationRewardedVideoAdListener.onVideoStarted(ChartboostAdapter.this);
                    }
                }
            };

    /**
     * This method will return an error type that can be read by Google Mobile Ads SDK.
     *
     * @param error CBImpressionError type to be translated to Google Mobile Ads SDK readable
     *              error code.
     * @return Ad request error code.
     */
    private static int getAdRequestErrorType(CBError.CBImpressionError error) {
        switch (error) {
            case INTERNAL:
            case INVALID_RESPONSE:
            case NO_HOST_ACTIVITY:
            case USER_CANCELLATION:
            case WRONG_ORIENTATION:
            case ERROR_PLAYING_VIDEO:
            case ERROR_CREATING_VIEW:
            case SESSION_NOT_STARTED:
            case ERROR_DISPLAYING_VIEW:
            case ERROR_LOADING_WEB_VIEW:
            case INCOMPATIBLE_API_VERSION:
            case ASSET_PREFETCH_IN_PROGRESS:
            case IMPRESSION_ALREADY_VISIBLE:
            case ACTIVITY_MISSING_IN_MANIFEST:
            case WEB_VIEW_CLIENT_RECEIVED_ERROR:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
            case NETWORK_FAILURE:
            case END_POINT_DISABLED:
            case INTERNET_UNAVAILABLE:
            case TOO_MANY_CONNECTIONS:
            case ASSETS_DOWNLOAD_FAILURE:
            case WEB_VIEW_PAGE_LOAD_TIMEOUT:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case INVALID_LOCATION:
            case VIDEO_ID_MISSING:
            case HARDWARE_ACCELERATION_DISABLED:
            case FIRST_SESSION_INTERSTITIALS_DISABLED:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case INTERNET_UNAVAILABLE_AT_SHOW:
            case NO_AD_FOUND:
            case ASSET_MISSING:
            case VIDEO_UNAVAILABLE:
            case EMPTY_LOCAL_VIDEO_LIST:
            case PENDING_IMPRESSION_ERROR:
            case VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION:
            default:
                return AdRequest.ERROR_CODE_NO_FILL;
        }
    }

    /**
     * This method will create and return a new {@link ChartboostParams} object populated with the
     * parameters obtained from the server parameters and network extras bundles.
     *
     * @param serverParameters a {@link Bundle} containing server parameters used to initialize
     *                         Chartboost.
     * @param networkExtras    a {@link Bundle} containing optional information to be used by the
     *                         adapter.
     * @return a {@link ChartboostParams} object populated with the params obtained from the
     * bundles provided.
     */
    private ChartboostParams createChartboostParams(Bundle serverParameters, Bundle networkExtras) {
        ChartboostParams params = new ChartboostParams();
        String appId = serverParameters.getString(KEY_APP_ID);
        String appSignature = serverParameters.getString(KEY_APP_SIGNATURE);
        if (appId != null && appSignature != null) {
            params.setAppId(appId.trim());
            params.setAppSignature(appSignature.trim());
        }

        String adLocation = serverParameters.getString(KEY_AD_LOCATION);
        if (!isValidParam(adLocation)) {
            // Ad Location is empty, log a warning and use the default location.
            Log.w(TAG, String.format("Chartboost ad location is empty, defaulting to %s. Please "
                            + "set the Ad Location parameter in your AdMob console.",
                    CBLocation.LOCATION_DEFAULT));
            adLocation = CBLocation.LOCATION_DEFAULT;
        }
        params.setLocation(adLocation.trim());

        if (networkExtras != null) {
            if (networkExtras.containsKey(ChartboostExtrasBundleBuilder.KEY_FRAMEWORK)
                    && networkExtras.containsKey(
                    ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION)) {
                mChartboostParams.setFramework((CBFramework) networkExtras.getSerializable(
                        ChartboostExtrasBundleBuilder.KEY_FRAMEWORK));
                mChartboostParams.setFrameworkVersion(
                        networkExtras.getString(
                                ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION));
            }
        }
        return params;
    }

    /**
     * This method will check whether or not the provided {@link ChartboostParams} is valid by
     * checking if the required parameters are available.
     *
     * @param params Chartboost params to be examined.
     * @return {@code true} if the given ChartboostParams' appId and appSignature are valid,
     * false otherwise.
     */
    private boolean isValidChartboostParams(ChartboostParams params) {
        String appId = params.getAppId();
        String appSignature = params.getAppSignature();
        if (!isValidParam(appId) || !isValidParam(appSignature)) {
            String log = !isValidParam(appId) ? (!isValidParam(appSignature)
                    ? "App ID and App Signature" : "App ID") : "App Signature";
            Log.w(TAG, log + " cannot be empty.");
            return false;
        }
        return true;
    }

    /**
     * This method will check whether or not the Chartboost parameter string provided is valid.
     *
     * @param string the string to be examined.
     * @return {@code true} if the param string is not null and length when trimmed is not
     * zero, {@code false} otherwise.
     */
    private static boolean isValidParam(String string) {
        return !(string == null || string.trim().length() == 0);
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle networkExtras) {
        mMediationInterstitialListener = mediationInterstitialListener;

        mChartboostParams = createChartboostParams(serverParameters, networkExtras);
        if (!isValidChartboostParams(mChartboostParams)) {
            // Invalid server parameters, send ad failed to load event.
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!ChartboostSingleton.startChartboostInterstitial(context,
                mChartboostInterstitialDelegate)) {
            // Chartboost initialization failed, send failed to load event.
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(ChartboostAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        // Request ChartboostSingleton to load interstitial ads.
        mIsLoading = true;
        ChartboostSingleton.loadInterstitialAd(mChartboostInterstitialDelegate);
    }

    @Override
    public void showInterstitial() {
        // Request ChartboostSingleton to show interstitial ads.
        ChartboostSingleton.showInterstitialAd(mChartboostInterstitialDelegate);
    }

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;

        mChartboostParams = createChartboostParams(serverParameters, networkExtras);
        if (!isValidChartboostParams(mChartboostParams)) {
            // Invalid server parameters, send initialization failed event.
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(ChartboostAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!ChartboostSingleton.startChartboostRewardedVideo(context,
                mChartboostRewardedVideoDelegate)) {
            // Chartboost initialization failed, send initialization failed event.
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(ChartboostAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        // Update mChartboostParams before loading ads.
        mChartboostParams = createChartboostParams(serverParameters, networkExtras);
        // Request singleton to load rewarded video ad.
        mIsLoading = true;
        ChartboostSingleton.loadRewardedVideoAd(mChartboostRewardedVideoDelegate);
    }

    @Override
    public void showVideo() {
        // Request ChartboostSingleton to show rewarded video ad.
        ChartboostSingleton.showRewardedVideoAd(mChartboostRewardedVideoDelegate);
    }

    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /**
     * The {@link com.google.ads.mediation.chartboost.ChartboostAdapter
     * .ChartboostExtrasBundleBuilder} class is used to create a networkExtras bundle which can
     * be passed to the adapter to make network specific customizations.
     */
    public static final class ChartboostExtrasBundleBuilder {

        /**
         * Key to add and obtain {@link #cbFramework}.
         */
        private static final String KEY_FRAMEWORK = "framework";

        /**
         * Key to add and obtain {@link #cbFrameworkVersion}.
         */
        private static final String KEY_FRAMEWORK_VERSION = "framework_version";

        /**
         * Framework being used to load Charboost ads.
         */
        private CBFramework cbFramework;

        /**
         * The version name of {@link #cbFramework}.
         */
        private String cbFrameworkVersion;

        public ChartboostExtrasBundleBuilder setFramework(CBFramework framework,
                                                          String version) {
            this.cbFramework = framework;
            this.cbFrameworkVersion = version;
            return this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_FRAMEWORK, cbFramework);
            bundle.putString(KEY_FRAMEWORK_VERSION, cbFrameworkVersion);
            return bundle;
        }
    }
}
