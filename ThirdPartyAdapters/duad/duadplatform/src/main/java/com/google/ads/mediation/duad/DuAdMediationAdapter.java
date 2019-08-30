package com.google.ads.mediation.duad;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.AdError;
import com.duapps.ad.video.AdResult;
import com.duapps.ad.video.DuVideoAd;
import com.duapps.ad.video.DuVideoAdListener;
import com.duapps.ad.video.DuVideoAdSDK;
import com.duapps.ad.video.DuVideoAdsManager;
import com.google.ads.mediation.dap.BuildConfig;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.util.HashSet;
import java.util.List;

public class DuAdMediationAdapter extends Adapter
        implements MediationRewardedAd, DuVideoAdListener {

    private static final String TAG = DuAdMediationAdapter.class.getSimpleName();

    private boolean mIsLoading = false;
    private DuVideoAd mRewardedAd;

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
        int micro = Integer.parseInt(splits[2]) * 10000
                + Integer.parseInt(splits[3]) * 100
                + Integer.parseInt(splits[4]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = DuVideoAdSDK.getVersionName();
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

        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            initializationCompleteCallback.onInitializationFailed(
                    "Unable to find the DU Video Ad SDK. Please ensure you have integrated "
                            + "the latest version of the DU Video Ad SDK.");
            return;
        }

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed(
                    "DU Ad SDK requires an Activity context to initialize.");
            return;
        }

        HashSet<String> appIDs = new HashSet<>();

        HashSet<Integer> nativePlacementIDs = new HashSet<>();
        HashSet<Integer> videoPlacementIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();

            String placementID = serverParameters.getString(DuAdMediation.KEY_DAP_PID);
            if (!TextUtils.isEmpty(placementID)) {
                try {
                    if (configuration.getFormat() == AdFormat.REWARDED) {
                        videoPlacementIDs.add(Integer.parseInt(placementID));
                    } else {
                        nativePlacementIDs.add(Integer.parseInt(placementID));
                    }
                } catch (NumberFormatException ex) {
                    initializationCompleteCallback.onInitializationFailed(
                            "Initialization failed: Invalid Placement IDs found.");
                    return;
                }
            }

            String appIDFromServer = serverParameters.getString(DuAdMediation.KEY_APP_ID);
            if (!TextUtils.isEmpty(appIDFromServer)) {
                appIDs.add(appIDFromServer);
            }
        }

        if ((nativePlacementIDs.size() + videoPlacementIDs.size()) <= 0) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: Missing or Invalid Placement IDs found.");
            return;
        }

        int count = appIDs.size();
        if (count > 0) {
            String appID = appIDs.iterator().next();

            if (count > 1) {
                String logMessage = String.format("Multiple '%s' entries found: %s. " +
                                "Using '%s' to initialize the DU Ad and DU Video SDK.",
                        DuAdMediation.KEY_APP_ID, appIDs, appID);
                Log.w(TAG, logMessage);
            }

            DuAdMediation.initializeSDK(context, appID, nativePlacementIDs, videoPlacementIDs);
            initializationCompleteCallback.onInitializationSucceeded();
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: Missing or Invalid App ID.");
        }
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        if (!DuAdMediation.checkClassExist("com.duapps.ad.video.DuVideoAdsManager")) {
            String logMessage = "Unable to find the DU Video Ad SDK. Please ensure you have "
                    + "integrated the latest version of the DU Video Ad SDK.";
            Log.e(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "DU Ad SDK requires an Activity context to initialize.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }
        Activity activity = (Activity) context;

        DuAdMediation.setDebug(mediationRewardedAdConfiguration.isTestRequest());
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        Bundle networkExtras = mediationRewardedAdConfiguration.getMediationExtras();

        int placementID = DuAdMediation.getValidPid(serverParameters);
        if (placementID < 0) {
            String logMessage =
                    "Failed to request ad from DU Ad Platform: Missing or Invalid Placement ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        String appID = serverParameters.getString(DuAdMediation.KEY_APP_ID);
        if (TextUtils.isEmpty(appID)) {
            String logMessage =
                    "Failed to request ad from DU Ad Platform: Missing or Invalid App ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        DuAdMediation.configureSDKForVideo(context, networkExtras, appID, placementID);

        mAdLoadCallback = mediationAdLoadCallback;
        mRewardedAd = DuVideoAdsManager.getVideoAd(activity, placementID);
        mRewardedAd.addListener(DuAdMediationAdapter.this);
        mRewardedAd.load();
        mIsLoading = true;
    }

    @Override
    public void showAd(Context context) {
        if (mRewardedAd != null && mRewardedAd.isAdPlayable()) {
            DuAdMediation.debugLog(TAG, "Dap Rewarded Video is available. Showing...");
            mRewardedAd.setListener(DuAdMediationAdapter.this);
            mRewardedAd.playAd(context);
        } else {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow(
                        "Dap Rewarded Video is not available. Try re-requesting.");
            }
        }
    }

    /**
     * {@link DuVideoAdListener} implementation
     */
    @Override
    public void onAdPlayable() {
        if (mIsLoading) {
            mIsLoading = false;

            if (mAdLoadCallback != null) {
                mRewardedAdCallback = mAdLoadCallback.onSuccess(DuAdMediationAdapter.this);
            }
        }
    }

    @Override
    public void onAdError(AdError adError) {
        if (mIsLoading) {
            mIsLoading = false;

            String logMessage = "Failed to request ad from DU Ad: " + adError.getErrorMessage();
            Log.w(TAG, logMessage);
            if (mAdLoadCallback != null) {
                mAdLoadCallback.onFailure(logMessage);
            }
            mRewardedAd.removeListener(DuAdMediationAdapter.this);
        }
    }

    @Override
    public void onAdStart() {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.onVideoStart();
            mRewardedAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onAdClick() {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onVideoCompleted() {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();
        }
    }

    @Override
    public void onAdClose() {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
        mRewardedAd.removeListener(DuAdMediationAdapter.this);
    }

    @Override
    public void onAdEnd(AdResult adResult) {
        if (mRewardedAdCallback != null) {
            if (adResult.isSuccessfulView()) {
                mRewardedAdCallback.onUserEarnedReward(null);
            }
        }
    }
}
