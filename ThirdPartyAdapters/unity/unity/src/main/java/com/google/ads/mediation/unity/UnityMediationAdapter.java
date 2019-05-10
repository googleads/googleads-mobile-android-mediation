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
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.unity3d.ads.UnityAds;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

/**
 * The {@link UnityMediationAdapter} is used to initialize the Unity Ads SDK, load rewarded
 * video ads from Unity ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class UnityMediationAdapter extends Adapter
    implements MediationRewardedAd, OnContextChangedListener {

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
     * Mediation rewarded video ad listener used to forward ad load status
     * from {@link UnitySingleton} to the Google Mobile Ads SDK.
     */
    private MediationAdLoadCallback<MediationRewardedAd,
            MediationRewardedAdCallback> mMediationAdLoadCallback;

    /**
     * Mediation rewarded video ad listener used to forward rewarded ad events
     * from {@link UnitySingleton} to the Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

    /**
     * Placement ID used to determine what type of ad to load.
     */
    private String mPlacementId;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    WeakReference<Activity> mActivityWeakReference;

    /**
     * Unity adapter delegate to to forward the events from {@link UnitySingleton} to Google Mobile
     * Ads SDK.
     */
    private UnityAdapterDelegate mUnityAdapterRewardedAdDelegate = new UnityAdapterDelegate() {

        @Override
        public String getPlacementId() {
            return mPlacementId;
        }

        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send Ad Loaded event if the
            // adapter is currently loading ads.
            if (placementId.equals(getPlacementId()) && mMediationAdLoadCallback != null) {
                mMediationRewardedAdCallback = mMediationAdLoadCallback
                        .onSuccess(UnityMediationAdapter.this);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
            // video adapter.
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onVideoStart();
            }
        }

        @Override
        public void onUnityAdsClick(String s) {
            // Unity Ads ad clicked.
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.reportAdClicked();
            }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
            // This callback is not forwarded to the adapter by the UnitySingleton and the
            // adapter should use the onUnityAdsReady and onUnityAdsError callbacks to forward
            // Unity Ads SDK state to Google Mobile Ads SDK.
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            // Unity Ads ad closed.
            if (mMediationRewardedAdCallback != null) {
                // Reward is provided only if the ad is watched completely.
                if (finishState == UnityAds.FinishState.COMPLETED) {
                    mMediationRewardedAdCallback.onVideoComplete();
                    // Unity Ads doesn't provide a reward value. The publisher is expected to
                    // override the reward in AdMob console.
                    mMediationRewardedAdCallback.onUserEarnedReward(new UnityReward());
                }
                mMediationRewardedAdCallback.onAdClosed();
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // Send Ad Failed to load event only if the adapter is currently loading ads.
            if (placementId.equals(getPlacementId()) && mMediationAdLoadCallback != null) {
                String logMessage =
                        "Failed to load Rewarded ad from Unity Ads: " + unityAdsError.toString();
                Log.w(TAG, logMessage);
                mMediationAdLoadCallback.onFailure(logMessage);
            }
        }
    };

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
        String versionString = UnityAds.getVersion();
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

        UnitySingleton.initializeUnityAds((Activity) context, gameID);
        initializationCompleteCallback.onInitializationSucceeded();
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                               MediationAdLoadCallback<MediationRewardedAd,
                                       MediationRewardedAdCallback> mediationAdLoadCallback) {
        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            mediationAdLoadCallback.onFailure("Context is not an Activity." +
                    " Unity Ads requires an Activity context to show ads.");
            return;
        }

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        String gameID = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameID, mPlacementId)) {
            mediationAdLoadCallback.onFailure("Failed to load ad from UnityAds: " +
                    "Missing or invalid game ID and placement ID.");
            return;
        }

        mMediationAdLoadCallback = mediationAdLoadCallback;

        if (UnityAds.isInitialized()) {
            // Request UnitySingleton to load ads for mPlacementId.
            UnitySingleton.loadAd(mUnityAdapterRewardedAdDelegate);
        } else {
            UnitySingleton.initializeUnityAds(mUnityAdapterRewardedAdDelegate,
                    (Activity) context, gameID, mPlacementId);
        }
    }

    @Override
    public void showAd(Context context) {
        if (!(context instanceof Activity)) {
            String message = "An activity context is required to show Unity Ads.";
            Log.w(TAG, message);
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdFailedToShow(message);
            }
            return;
        }

        // Request UnitySingleton to show video ads.
        if (UnityAds.isReady(mPlacementId)) {
            UnitySingleton.showAd(mUnityAdapterRewardedAdDelegate, (Activity) context);

            // Unity Ads does not have an ad opened callback.
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdOpened();
                mMediationRewardedAdCallback.reportAdImpression();
            }
        } else {
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdFailedToShow(
                        "UnityAds placement '" + mPlacementId + "' is not ready.");
            }
        }

    }

    @Override
    public void onContextChanged(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity." +
                    "Unity Ads requires an Activity context to show ads.");
            return;
        }

        // Storing a weak reference of the current Activity to be used when showing an ad.
        mActivityWeakReference = new WeakReference<>((Activity) context);
    }

    /**
     * Checks whether or not the provided Unity Ads IDs are valid.
     *
     * @param gameId      Unity Ads Game ID to be verified.
     * @param placementId Unity Ads Placement ID to be verified.
     * @return {@code true} if all the IDs provided are valid.
     */
    private static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, ids + " cannot be empty.");

            return false;
        }

        return true;
    }
}
