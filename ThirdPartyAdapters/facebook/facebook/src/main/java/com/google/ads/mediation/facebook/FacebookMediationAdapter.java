package com.google.ads.mediation.facebook;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;

import java.util.ArrayList;
import java.util.List;

public class FacebookMediationAdapter extends Adapter implements MediationRewardedAd {

    static final String TAG = FacebookAdapter.class.getSimpleName();

    static final String PLACEMENT_PARAMETER = "pubid";

    /**
     * Facebook rewarded video ad instance.
     */
    private RewardedVideoAd mRewardedVideoAd;

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
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
        String versionString = com.facebook.ads.BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(final Context context,
                           final InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (context == null) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: Context is null.");
            return;
        }

        ArrayList<String> placements = new ArrayList<>();
        for (MediationConfiguration adConfiguration : mediationConfigurations) {
            Bundle serverParameters = adConfiguration.getServerParameters();

            String placementID = serverParameters.getString(PLACEMENT_PARAMETER);
            if (!TextUtils.isEmpty(placementID)) {
                placements.add(placementID);
            }
        }

        if (placements.isEmpty()) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: No placement IDs found");
            return;
        }

        FacebookInitializer.getInstance().initialize(context, placements,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void onInitializeError(String message) {
                initializationCompleteCallback.onInitializationFailed(
                        "Initialization failed: " + message);
            }
        });
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {

        final Context context = mediationRewardedAdConfiguration.getContext();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

        if (!isValidRequestParameters(context, serverParameters)) {
            mediationAdLoadCallback.onFailure("Invalid request");
            return;
        }

        mAdLoadCallback = mediationAdLoadCallback;
        final String placementID = serverParameters.getString(PLACEMENT_PARAMETER);

        FacebookInitializer.getInstance().initialize(context, placementID,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                createAndLoadRewardedVideo(context, placementID);
            }

            @Override
            public void onInitializeError(String message) {
                String logMessage = "Failed to load ad from Facebook: " + message;
                Log.w(TAG, logMessage);
                if (mAdLoadCallback != null) {
                    mAdLoadCallback.onFailure(logMessage);
                }
            }
        });
    }

    @Override
    public void showAd(Context context) {
        if (mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded()) {
            mRewardedVideoAd.show();
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdOpened();
                mRewardedAdCallback.onVideoStart();
            }
        } else {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow("No ads to show.");
            }
        }
    }

    //region Rewarded video adapter utility methods and classes.
    private void createAndLoadRewardedVideo(Context context, String placementID) {
        mRewardedVideoAd = new RewardedVideoAd(context, placementID);
        mRewardedVideoAd.setAdListener(
                new RewardedVideoListener(mRewardedVideoAd, mAdLoadCallback));
        mRewardedVideoAd.loadAd(true);
    }

    /**
     * A {@link RewardedVideoAdListener} used to listen to rewarded video ad events from Facebook
     * SDK and forward to Google Mobile Ads SDK using {@link #mRewardedAdCallback}
     */
    private class RewardedVideoListener implements RewardedVideoAdListener {
        private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mMediationAdLoadCallback;
        private RewardedVideoAd mRewardedVideoAd;

        private RewardedVideoListener(RewardedVideoAd rewardedVideoAd,
                MediationAdLoadCallback<MediationRewardedAd,
                MediationRewardedAdCallback> mMediationAdLoadCallback) {
            this.mRewardedVideoAd = rewardedVideoAd;
            this.mMediationAdLoadCallback = mMediationAdLoadCallback;
        }

        @Override
        public void onRewardedVideoCompleted() {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onVideoComplete();
                // Facebook SDK doesn't provide a reward value. The publisher is expected to
                // override the reward in AdMob UI.
                mRewardedAdCallback.onUserEarnedReward(new FacebookReward());
            }
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            String errorMessage = adError.getErrorMessage();
            if (!TextUtils.isEmpty(errorMessage)) {
                Log.w(TAG, "Failed to load ad from Facebook: " + errorMessage);
            }

            if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(errorMessage);
            }

            mRewardedVideoAd.destroy();
        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (mMediationAdLoadCallback != null) {
                mRewardedAdCallback =
                        mMediationAdLoadCallback.onSuccess(FacebookMediationAdapter.this);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.reportAdClicked();
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.reportAdImpression();
            }
        }

        @Override
        public void onRewardedVideoClosed() {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdClosed();
            }
            mRewardedVideoAd.destroy();
        }
    }

    /**
     * An implementation of {@link RewardItem} that will be given to the app when a Facebook reward
     * is granted. Because the FAN SDK doesn't provide reward amounts and types, defaults are used
     * here.
     */
    private class FacebookReward implements RewardItem {

        @Override
        public String getType() {
            // Facebook SDK does not provide a reward type.
            return "";
        }

        @Override
        public int getAmount() {
            // Facebook SDK does not provide reward amount, default to 1.
            return 1;
        }
    }
    //endregion

    //region Helper methods

    /**
     * Checks whether or not the request parameters needed to load Facebook ads are null.
     *
     * @param context          an Android {@link Context}.
     * @param serverParameters a {@link Bundle} containing server parameters needed to request ads
     *                         from Facebook.
     * @return {@code false} if any of the request parameters are null.
     */
    static boolean isValidRequestParameters(Context context, Bundle serverParameters) {
        if (context == null) {
            Log.w(TAG, "Failed to request ad, Context is null.");
            return false;
        }

        if (serverParameters == null) {
            Log.w(TAG, "Failed to request ad, serverParameters is null.");
            return false;
        }

        if (TextUtils.isEmpty(serverParameters.getString(PLACEMENT_PARAMETER))) {
            Log.w(TAG, "Failed to request ad, placementId is null or empty.");
            return false;
        }
        return true;
    }
    //endregion

}
