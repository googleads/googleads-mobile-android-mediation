package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

public class UnityRewardedAd implements MediationRewardedAd, IUnityAdsLoadListener, IUnityAdsExtendedListener {

    /**
     * Mediation rewarded video ad configuration to render ad.
     */
    private MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

    /**
     * Mediation rewarded video ad listener used to forward ad load status
     * to the Google Mobile Ads SDK.
     */
    private MediationAdLoadCallback<MediationRewardedAd,
            MediationRewardedAdCallback> mMediationAdLoadCallback;

    /**
     * Mediation rewarded video ad listener used to forward rewarded ad events
     * to the Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

    /**
     * Placement ID used to determine what type of ad to load.
     */
    private String mPlacementId;

    public UnityRewardedAd(MediationRewardedAdConfiguration adConfiguration,
                           MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        this.mediationRewardedAdConfiguration = adConfiguration;
        this.mMediationAdLoadCallback = callback;
    }

    /**
     * Returns the placement ID of the ad being loaded.
     *
     * @return mPlacementId.
     */
    public String getPlacementId() {
        return mPlacementId;
    }

    /**
     * Loads a rewarded ad.
     */
    public void load()
    {
        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            mMediationAdLoadCallback.onFailure("Context is not an Activity." +
                    " Unity Ads requires an Activity context to show ads.");
            return;
        }

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        String gameID = serverParameters.getString(UnityMediationAdapter.KEY_GAME_ID);
        mPlacementId = serverParameters.getString(UnityMediationAdapter.KEY_PLACEMENT_ID);

        if (!isValidIds(gameID, mPlacementId)) {
            mMediationAdLoadCallback.onFailure("Failed to load ad from UnityAds: " +
                    "Missing or invalid game ID and placement ID.");
            return;
        }

        UnityInitializer.getInstance().initializeUnityAds((Activity) context, gameID,
                new IUnityAdsInitializationListener() {
                    @Override
                    public void onInitializationComplete() {
                        Log.d(UnityAdapter.TAG, "Unity Ads successfully initialized");
                        loadRewardedAd(mPlacementId);
                    }

                    @Override
                    public void onInitializationFailed(UnityAds.UnityAdsInitializationError
                                                               unityAdsInitializationError, String s) {
                        Log.e(UnityAdapter.TAG, "Unity Ads initialization failed: [" +
                                unityAdsInitializationError + "] " + s);
                    }
                });
    }

    /**
     * This method will load Unity Ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     */
    protected void loadRewardedAd(String placementId) {

        UnityAds.load(placementId, this);

    }

    @Override
    public void showAd(Context context) {

        if (!(context instanceof Activity)) {
            String message = "An activity context is required to show Unity Ads.";
            Log.w(UnityAdapter.TAG, message);
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdFailedToShow(message);
            }
            return;
        }
        Activity activity = (Activity) context;

        // Request to show video ads.
        if (UnityAds.isReady(mPlacementId)) {

            // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
            // Unity Ads fails to show an ad).

            UnityAds.show(activity, mPlacementId);

            // Unity Ads does not have an ad opened callback.
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdOpened();
                mMediationRewardedAdCallback.reportAdImpression();
            }
        } else {
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdFailedToShow("UnityAds placement '" +
                        mPlacementId + "' is not ready.");
            }
        }

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
            Log.w(UnityAdapter.TAG, ids + " cannot be empty.");

            return false;
        }
        return true;
    }

    @Override
    public void onUnityAdsAdLoaded(String s) {
        Log.d(UnityAdapter.TAG, "Unity Ads rewarded ad successfully loaded " + s);
        if (mMediationAdLoadCallback != null) {
            mMediationRewardedAdCallback = mMediationAdLoadCallback
                    .onSuccess(this);
        }

    }

    @Override
    public void onUnityAdsFailedToLoad(String s) {
        Log.e(UnityAdapter.TAG, "Unity Ads rewarded ad load failure " +s);
        if (mMediationAdLoadCallback != null) {
            mMediationAdLoadCallback.onFailure(s);
        }
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        // Unity Ads is ready to show ads for the given placementId.
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        // Unity Ads video ad started playing. Send Video Started event if this is a rewarded
        // video.
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
        // This callback is not forwarded to Google Mobile Ads SDK. onUnityAdsError should be used
        // to forward Unity Ads SDK state to Google Mobile Ads SDK.
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
        // Unity Ads ad failed to show.
        if (placementId.equals(getPlacementId())) {
            String logMessage =
                    "Failed to show Rewarded ad from Unity Ads: " + unityAdsError.toString();
            Log.w(UnityAdapter.TAG, logMessage);
            mMediationRewardedAdCallback.onAdFailedToShow(logMessage);
        }

        // check with google
    }
}
