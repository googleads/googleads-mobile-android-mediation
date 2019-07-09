package com.google.ads.mediation.mopub;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.dfp.adapters.BuildConfig;

import java.util.List;
import java.util.Set;

/**
 * A {@link com.google.ads.mediation.mopub.MoPubMediationAdapter} used to mediate rewarded video
 * ads from MoPub.
 */
public class MoPubMediationAdapter extends Adapter
        implements MediationRewardedAd, MoPubRewardedVideoListener {

    static final String TAG = MoPubMediationAdapter.class.getSimpleName();
    private static final String MOPUB_AD_UNIT_KEY = "adUnitId";

    private String adUnitID = "";

    // TODO: Remove `adExpired` parameter once MoPub fixes MoPubRewardedVideos.hasRewardedVideo()
    // to return false for expired ads.
    private boolean adExpired;

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mAdLoadCallback;
    private MediationRewardedAdCallback mRewardedAdCallback;

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
        String versionString = MoPub.SDK_VERSION;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           final InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("MoPub SDK requires an " +
                    "Activity context to initialize.");
            return;
        }

        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();

            // The MoPub SDK requires any valid Ad Unit ID in order to initialize their SDK.
            adUnitID = serverParameters.getString(MOPUB_AD_UNIT_KEY);
            if (!TextUtils.isEmpty(adUnitID)) {
                break;
            }
        }

        if (TextUtils.isEmpty(adUnitID)) {
            initializationCompleteCallback.onInitializationFailed("Initialization failed: " +
                    "Missing or Invalid MoPub Ad Unit ID.");
            return;
        }

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnitID).build();
        MoPubSingleton.getInstance().initializeMoPubSDK((Activity) context, sdkConfiguration,
                new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                initializationCompleteCallback.onInitializationSucceeded();
            }
        });
    }

    @Override
    public void loadRewardedAd(
            final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        Context context = mediationRewardedAdConfiguration.getContext();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        adUnitID = serverParameters.getString(MOPUB_AD_UNIT_KEY);

        if (TextUtils.isEmpty(adUnitID)) {
            String logMessage = "Failed to request ad from MoPub: "
                    + "Missing or Invalid MoPub Ad Unit ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mAdLoadCallback = mediationAdLoadCallback;
        MoPubRewardedVideoManager.RequestParameters requestParameters =
                new MoPubRewardedVideoManager.RequestParameters(
                        MoPubSingleton.getKeywords(mediationRewardedAdConfiguration, false),
                        MoPubSingleton.getKeywords(mediationRewardedAdConfiguration, true),
                        mediationRewardedAdConfiguration.getLocation()
                );
        MoPubSingleton.getInstance().loadRewardedAd(
                context, adUnitID, requestParameters, MoPubMediationAdapter.this);
    }

    @Override
    public void showAd(Context context) {
        if (adExpired && mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("Failed to show a MoPub rewarded video. " +
                    "The MoPub Ad has expired. Please make a new Ad Request.");
        } else if (!adExpired) {
            boolean didShow = MoPubSingleton.getInstance().showRewardedAd(adUnitID);
            if (!didShow && mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow("Failed to show a MoPub rewarded video. " +
                        "Either the video is not ready or the ad unit ID is empty.");
            }
        }
    }

    /**
     * {@link MoPubRewardedVideoListener} implementation
     */
    @Override
    public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
        if (mAdLoadCallback != null) {
            mRewardedAdCallback = mAdLoadCallback.onSuccess(MoPubMediationAdapter.this);
        }
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull String adUnitId,
                                           @NonNull MoPubErrorCode errorCode) {
        if (mAdLoadCallback != null) {
            String logMessage = "Failed to load MoPub rewarded video: ";
            switch (errorCode) {
                case NO_FILL:
                    logMessage += "No Fill.";
                    break;
                case NETWORK_TIMEOUT:
                    logMessage += "Network error.";
                    break;
                case SERVER_ERROR:
                    logMessage += "Invalid Request.";
                    break;
                case EXPIRED:
                    // MoPub Rewarded video ads expire after 4 hours.
                    MoPubSingleton.getInstance().adExpired(adUnitId, MoPubMediationAdapter.this);
                    adExpired = true;
                    logMessage += "The MoPub Ad has expired. Please make a new Ad Request.";
                    break;
                default:
                    logMessage += "Internal Error.";
                    break;
            }
            Log.i(TAG, logMessage);
            mAdLoadCallback.onFailure(logMessage);
        }
    }

    @Override
    public void onRewardedVideoStarted(@NonNull String adUnitId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.onVideoStart();
        }
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("Failed to playback MoPub rewarded video: " +
                    errorCode.toString());
        }
    }

    @Override
    public void onRewardedVideoClicked(@NonNull String adUnitId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds,
                                         @NonNull final MoPubReward reward) {
        Preconditions.checkNotNull(reward);

        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();
            mRewardedAdCallback.onUserEarnedReward(new RewardItem() {
                @Override
                public String getType() {
                    return reward.getLabel();
                }

                @Override
                public int getAmount() {
                    return reward.getAmount();
                }
            });
        }
    }

    @Override
    public void onRewardedVideoClosed(@NonNull String adUnitId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
    }
}
