package com.google.ads.mediation.chartboost;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.util.HashMap;
import java.util.List;

public class ChartboostMediationAdapter extends Adapter implements MediationRewardedAd {

    static final String TAG = ChartboostMediationAdapter.class.getSimpleName();

    /**
     * A Chartboost extras object used to store optional information used when loading ads.
     */
    private ChartboostParams mChartboostParams = new ChartboostParams();

    /**
     * Flag to keep track of whether or not this {@link ChartboostMediationAdapter} is loading ads.
     */
    private boolean mIsLoading;

    private InitializationCompleteCallback mInitializationCallback;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mAdLoadCallback;
    private MediationRewardedAdCallback mRewardedAdCallback;

    /**
     * {@link Adapter} implementation
     */
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = Chartboost.getSDKVersion();
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {
        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed(
                    "Chartboost SDK requires an Activity context to initialize");
            return;
        }

        HashMap<String, Bundle> chartboostConfigs = new HashMap<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle params = configuration.getServerParameters();
            String serverAppID = params.getString(ChartboostAdapterUtils.KEY_APP_ID);

            if (!TextUtils.isEmpty(serverAppID)) {
                chartboostConfigs.put(serverAppID, params);
            }
        }

        String appID;
        Bundle serverParameters;
        int count = chartboostConfigs.size();
        if (count > 0) {
            appID = chartboostConfigs.keySet().iterator().next();
            serverParameters = chartboostConfigs.get(appID);

            if (count > 1) {
                String logMessage = String.format("Multiple '%s' entries found: %s. "
                                + "Using '%s' to initialize the Chartboost SDK",
                        ChartboostAdapterUtils.KEY_APP_ID, chartboostConfigs.keySet(), appID);
                Log.w(TAG, logMessage);
            }
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed:Missing or invalid App ID.");
            return;
        }

        mInitializationCallback = initializationCompleteCallback;

        mChartboostParams = ChartboostAdapterUtils.createChartboostParams(serverParameters, null);
        if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
            // Invalid server parameters, send initialization failed event.
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: Invalid server parameters.");
            return;
        }

        ChartboostSingleton.startChartboostRewardedVideo(
                context, mChartboostRewardedVideoDelegate);
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        mAdLoadCallback = mediationAdLoadCallback;
        final Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        final Bundle extras = mediationRewardedAdConfiguration.getMediationExtras();

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "Failed to request ad from Chartboost: "
                    + "Chartboost SDK requires an Activity context to initialize.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mChartboostParams = ChartboostAdapterUtils
                .createChartboostParams(serverParameters, extras);
        if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
            // Invalid server parameters, send initialization failed event.
            String logMessage =
                    "Failed to request ad from Chartboost: Invalid server parameters.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        if (!ChartboostAdapterUtils.isValidContext(context)) {
            // Chartboost initialization failed, send initialization failed event.
            String logMessage = "Failed to request ad from Chartboost: Internal Error.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        ChartboostSingleton.startChartboostRewardedVideo(context,
                mChartboostRewardedVideoDelegate);
    }

    @Override
    public void showAd(Context context) {
        ChartboostSingleton.showRewardedVideoAd(mChartboostRewardedVideoDelegate);
    }

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
                    if (mInitializationCallback != null) {
                        mInitializationCallback.onInitializationSucceeded();
                    }

                    // If 'mAdLoadCallback' is not null, then it means an Ad request is pending
                    // to be sent after initializing.
                    if (mAdLoadCallback != null) {
                        mIsLoading = true;
                        ChartboostSingleton.loadRewardedVideoAd(mChartboostRewardedVideoDelegate);
                    }
                }

                @Override
                public void didCacheRewardedVideo(String location) {
                    super.didCacheRewardedVideo(location);
                    if (mAdLoadCallback != null && mIsLoading
                            && location.equals(mChartboostParams.getLocation())) {
                        mIsLoading = false;
                        mRewardedAdCallback = mAdLoadCallback
                                .onSuccess(ChartboostMediationAdapter.this);
                    }
                }

                @Override
                public void didFailToLoadRewardedVideo(String location,
                                                       CBError.CBImpressionError error) {
                    super.didFailToLoadRewardedVideo(location, error);
                    if (mAdLoadCallback != null &&
                            location.equals(mChartboostParams.getLocation())) {
                        if (mIsLoading) {
                            String logMessage =
                                    "Failed to load ad from Chartboost: " + error.toString();
                            Log.w(TAG, logMessage);
                            mAdLoadCallback.onFailure(logMessage);
                            mIsLoading = false;
                        } else if (error
                                == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
                            // Chartboost sends the CBErrorInternetUnavailableAtShow error when
                            // the Chartboost SDK fails to show an ad because no network connection
                            // is available.
                            if (mRewardedAdCallback != null) {
                                mRewardedAdCallback.onAdFailedToShow(
                                        "No network connection is available.");
                            }
                        }
                    }
                }

                @Override
                public void didDismissRewardedVideo(String location) {
                    super.didDismissRewardedVideo(location);
                    if (mRewardedAdCallback != null) {
                        mRewardedAdCallback.onAdClosed();
                    }
                }

                @Override
                public void didClickRewardedVideo(String location) {
                    super.didClickRewardedVideo(location);
                    if (mRewardedAdCallback != null) {
                        mRewardedAdCallback.reportAdClicked();
                    }
                }

                @Override
                public void didCompleteRewardedVideo(String location, int reward) {
                    super.didCompleteRewardedVideo(location, reward);
                    if (mRewardedAdCallback != null) {
                        mRewardedAdCallback.onVideoComplete();
                        mRewardedAdCallback.onUserEarnedReward(new ChartboostReward(reward));
                    }
                }

                @Override
                public void didDisplayRewardedVideo(String location) {
                    super.didDisplayRewardedVideo(location);
                    if (mRewardedAdCallback != null) {
                        // Charboost doesn't have a video started callback. We assume that the video
                        // started once the ad has been displayed.
                        mRewardedAdCallback.onAdOpened();
                        mRewardedAdCallback.onVideoStart();
                        mRewardedAdCallback.reportAdImpression();
                    }
                }
            };
}