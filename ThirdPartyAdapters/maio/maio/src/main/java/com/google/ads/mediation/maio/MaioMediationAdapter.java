package com.google.ads.mediation.maio;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import java.util.HashSet;
import java.util.List;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;
import jp.maio.sdk.android.mediation.admob.adapter.BuildConfig;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;

public class MaioMediationAdapter extends Adapter
        implements MediationRewardedAd, MaioAdsListenerInterface {

    public static final String TAG = MaioMediationAdapter.class.getSimpleName();

    protected String mMediaID;
    protected String mZoneID;

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
        String versionString = MaioAds.getSdkVersion();
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
            initializationCompleteCallback.onInitializationFailed(
                    "Maio SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> mediaIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            String mediaIDFromServer = configuration.getServerParameters()
                    .getString(MaioAdsManager.KEY_MEDIA_ID);

            if (!TextUtils.isEmpty(mediaIDFromServer)) {
                mediaIDs.add(mediaIDFromServer);
            }
        }

        int count = mediaIDs.size();
        if (count > 0) {
            String mediaID = mediaIDs.iterator().next();

            if (count > 1) {
                String logMessage = String.format("Multiple '%s' entries found: %s. "
                        + "Using '%s' to initialize the Maio SDK",
                        MaioAdsManager.KEY_MEDIA_ID, mediaIDs, mediaID);
                Log.w(TAG, logMessage);
            }

            MaioAdsManager.getManager(mediaID).initialize((Activity) context,
                    new MaioAdsManager.InitializationListener() {
                @Override
                public void onMaioInitialized() {
                    initializationCompleteCallback.onInitializationSucceeded();
                }
            });
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: Missing or Invalid Media ID.");
        }
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "Failed to request ad from Maio: " +
                    "Maio SDK requires an Activity context to load ads.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

        mMediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
        if (TextUtils.isEmpty(mMediaID)) {
            String logMessage = "Failed to request ad from Maio: Missing or Invalid Media ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mZoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
        if (TextUtils.isEmpty(mMediaID)) {
            String logMessage = "Failed to request ad from Maio: Missing or Invalid Zone ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mAdLoadCallback = mediationAdLoadCallback;
        MaioAds.setAdTestMode(mediationRewardedAdConfiguration.isTestRequest());
        MaioAdsManager.getManager(mMediaID).initialize((Activity) context,
                new MaioAdsManager.InitializationListener() {
            @Override
            public void onMaioInitialized() {
                MaioAdsManager.getManager(mMediaID).loadAd(mZoneID, MaioMediationAdapter.this);
            }
        });
    }

    @Override
    public void showAd(Context context) {
        boolean didShow = MaioAdsManager.getManager(mMediaID).showAd(mZoneID);
        if (!didShow && mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("Ad not ready for zone ID: " + mZoneID);
        }
    }

    //region MaioAdsListenerInterface implementation
    @Override
    public void onInitialized() {
        // Not called.
        // MaioAdsManager calls MaioAdsManager.InitializationListener.onMaioInitialized() instead.
    }

    @Override
    public void onChangedCanShow(String zoneId, boolean isAvailable) {
        if (mAdLoadCallback != null && isAvailable) {
            mRewardedAdCallback = mAdLoadCallback.onSuccess(MaioMediationAdapter.this);
        }
    }

    @Override
    public void onFailed(FailNotificationReason reason, String zoneId) {
        if (mAdLoadCallback != null) {
            String logMessage = "Failed to request ad from Maio: " + reason.toString();
            Log.w(TAG, logMessage);
            mAdLoadCallback.onFailure(logMessage);
        }
    }

    @Override
    public void onOpenAd(String zoneId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onStartedAd(String zoneId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoStart();
        }
    }

    @Override
    public void onClickedAd(String zoneId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();
            if (!skipped) {
                mRewardedAdCallback.onUserEarnedReward(new MaioReward());
            }
        }
    }

    @Override
    public void onClosedAd(String zoneId) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
    }
    //endregion

    /**
     * A {@link RewardItem} used to map maio rewards to Google's rewarded video ads rewards.
     */
    private class MaioReward implements RewardItem {
        private MaioReward() {
        }

        @Override
        public int getAmount() {
            return 1;
        }

        @Override
        public String getType() {
            return "";
        }
    }
}
