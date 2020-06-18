package com.google.ads.mediation.unity;

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
import com.unity3d.ads.BuildConfig;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.HashSet;
import java.util.List;

/**
 * The {@link UnityMediationAdapter} is used to initialize the Unity Ads SDK, load rewarded
 * video ads from Unity Ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityMediationAdapter extends Adapter {

    /**
     * TAG used for logging messages.
     */
    static final String TAG = UnityMediationAdapter.class.getSimpleName();

    /**
     * Key to obtain Game ID, required for loading Unity Ads.
     */
    static final String KEY_GAME_ID = "gameId";

    /**
     * Key to obtain Placement ID, used to set the type of ad to be shown. Unity Ads has changed
     * the name from Zone ID to Placement ID in Unity Ads SDK 2.0.0. To maintain backwards
     * compatibility the key is not changed.
     */
    static final String KEY_PLACEMENT_ID = "zoneId";

    /**
     * UnityRewardedAd instance.
     */
    private UnityRewardedAd rewardedAd;

    /**
     * {@link Adapter} implementation
     */
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected adapter version format: %s." +
                "Returning 0.0.0 for adapter version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = UnityAds.getVersion();
        String splits[] = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected SDK version format: %s." +
                "Returning 0.0.0 for SDK version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(Context context,
                           final InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {
        if (!(context instanceof Activity)){
            initializationCompleteCallback.onInitializationFailed("UnityAds SDK requires an " +
                    "Activity context to initialize");
            return;
        }

        HashSet<String> gameIDs = new HashSet<>();
        for (MediationConfiguration configuration: mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String gameIDFromServer = serverParameters.getString(KEY_GAME_ID);

            if (!TextUtils.isEmpty(gameIDFromServer)) {
                gameIDs.add(gameIDFromServer);
            }
        }

        String gameID = "";
        int count = gameIDs.size();
        if (count > 0) {
            gameID = gameIDs.iterator().next();

            if (count > 1) {
                String message = String.format("Multiple '%s' entries found: %s. " +
                                "Using '%s' to initialize the UnityAds SDK",
                        KEY_GAME_ID, gameIDs.toString(), gameID);
                Log.w(TAG, message);
            }
        }

        if (TextUtils.isEmpty(gameID)) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: Missing or invalid Game ID.");
            return;
        }


        UnityInitializer.getInstance().initializeUnityAds((Activity) context, gameID,
                    new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    initializationCompleteCallback.onInitializationSucceeded();
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError
                                                           unityAdsInitializationError, String s) {
                    initializationCompleteCallback.onInitializationFailed(
                            "Initialization failed: " + s);
                }
        });
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                               MediationAdLoadCallback<MediationRewardedAd,
                                       MediationRewardedAdCallback> mediationAdLoadCallback) {
        rewardedAd = new UnityRewardedAd(mediationRewardedAdConfiguration,
                mediationAdLoadCallback);
        rewardedAd.load();
    }

}
