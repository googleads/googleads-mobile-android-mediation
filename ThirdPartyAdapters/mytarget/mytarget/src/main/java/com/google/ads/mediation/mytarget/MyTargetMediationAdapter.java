package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
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
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.InterstitialAd.InterstitialAdListener;
import com.my.target.common.CustomParams;
import com.my.target.common.MyTargetVersion;

import java.util.List;

public class MyTargetMediationAdapter extends Adapter
        implements MediationRewardedAd, InterstitialAdListener {

    static final String TAG = MyTargetMediationAdapter.class.getSimpleName();

    private InterstitialAd mRewardedAd;

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
        String versionString = MyTargetVersion.VERSION;
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

        // MyTarget SDK does not have any API for initialization.
        initializationCompleteCallback.onInitializationSucceeded();
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        Context context = mediationRewardedAdConfiguration.getContext();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

        int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
        Log.d(TAG, "Requesting rewarded mediation, slotID: " + slotId);

        if (slotId < 0) {
            mediationAdLoadCallback.onFailure(
                    "Failed to request ad from MyTarget: Internal Error.");
            return;
        }

        mAdLoadCallback = mediationAdLoadCallback;

        mRewardedAd = new InterstitialAd(slotId, context);
        CustomParams params = mRewardedAd.getCustomParams();
        params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY,
                MyTargetTools.PARAM_MEDIATION_VALUE);
        mRewardedAd.setListener(MyTargetMediationAdapter.this);
        mRewardedAd.load();
    }

    @Override
    public void showAd(Context context) {
        Log.d(TAG, "Show video");
        if (mRewardedAd != null) {
            mRewardedAd.show();
        } else if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("Rewarded Video is null.");
        }
    }

    /**
     * A {@link InterstitialAdListener} used to forward myTarget rewarded video events to
     * Google.
     */
    @Override
    public void onLoad(@NonNull final InterstitialAd ad) {
        Log.d(TAG, "Ad loaded");
        if (mAdLoadCallback != null) {
            mRewardedAdCallback = mAdLoadCallback.onSuccess(MyTargetMediationAdapter.this);
        }
    }

    @Override
    public void onNoAd(@NonNull final String reason, @NonNull final InterstitialAd ad) {
        String logMessage = "Failed to load ad from MyTarget: " + reason;
        Log.i(TAG, logMessage);
        if (mAdLoadCallback != null) {
            mAdLoadCallback.onFailure(logMessage);
        }
    }

    @Override
    public void onClick(@NonNull final InterstitialAd ad) {
        Log.d(TAG, "Ad clicked");
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onDismiss(@NonNull final InterstitialAd ad) {
        Log.d(TAG, "Ad dismissed");
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
    }

    @Override
    public void onVideoCompleted(@NonNull final InterstitialAd ad) {
        Log.d(TAG, "Video completed");
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();
            mRewardedAdCallback.onUserEarnedReward(new MyTargetReward());
        }
    }

    @Override
    public void onDisplay(@NonNull final InterstitialAd ad) {
        Log.d(TAG, "Ad displayed");
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            // myTarget has no callback for starting video, but rewarded video always
            // has autoplay param and starts immediately, so we can notify Google about this.
            mRewardedAdCallback.onVideoStart();
            mRewardedAdCallback.reportAdImpression();
        }
    }

    /**
     * MyTarget doesn't provide reward for this moment, so we use this.
     */
    private static class MyTargetReward implements RewardItem {
        @Override
        public String getType() {
            return "";
        }

        @Override
        public int getAmount() {
            return 1;
        }
    }

}
