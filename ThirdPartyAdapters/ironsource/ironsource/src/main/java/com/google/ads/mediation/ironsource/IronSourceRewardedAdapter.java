package com.google.ads.mediation.ironsource;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import android.app.Activity;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.ironsource.mediationsdk.model.Placement;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;

import com.google.android.gms.ads.AdRequest;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;

import com.ironsource.mediationsdk.sdk.RewardedVideoListener;

/**
 * A {@link MediationRewardedVideoAdAdapter} to load and show myTarget rewarded video ads.
 */
public class IronSourceRewardedAdapter extends IronSourceBaseAdapter implements MediationRewardedVideoAdAdapter,
        RewardedVideoListener {

    /**
     * Key to obtain Rewarded Video placement name
     */
    private static final String KEY_RV_PLACEMENT = "rewardedVideoPlacement";

    /**
     * This is the placement name used for Rewarded Video
     */
    private String mRewardedVideoPlacementName;

    /**
     * Flag to keep track of whether or not this {@link IronSourceRewardedAdapter} is initialized.
     */
    private boolean mIsInitialized;

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    /**
     * Private IronSource methods
     */


    private void onRewardedVideoInitSuccess() {
        onLog("onRewardedVideoInitSuccess");

        if (mMediationRewardedVideoAdListener != null) {

            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onInitializationSucceeded");
                    mMediationRewardedVideoAdListener.onInitializationSucceeded(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    /**
     * MediationRewardedVideoAdAdapter implementation
     */

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        onLog("initialize");
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
        IronSource.setRewardedVideoListener(this);

        mRewardedVideoPlacementName = serverParameters.getString(KEY_RV_PLACEMENT, "");

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            onLog("IronSource SDK requires an Activity context to initialize");
            mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("onInitializationFailed, make sure that 'appKey' server parameter is added");
                mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);

                return;
            }

            // Everything is ok, continue with IronSource initialization
            onLog("Server params | appKey: " + appKey + " | isTestEnabled: " + mIsTestEnabled + " | placementName: " + mRewardedVideoPlacementName);
            initIronSourceSDK(context, serverParameters, IronSource.AD_UNIT.REWARDED_VIDEO);
            mIsInitialized = true;

            // in case of Rewarded Video ad unit we report init success
            onRewardedVideoInitSuccess();

        } catch (Exception e) {
            mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle networkExtras) {
        onLog("loadAd");

        if (IronSource.isRewardedVideoAvailable()) {
            onLog("onAdLoaded");
            mMediationRewardedVideoAdListener.onAdLoaded(IronSourceRewardedAdapter.this);
        }
    }

    @Override
    public void showVideo() {
        onLog("showVideo");

        if (TextUtils.isEmpty(mRewardedVideoPlacementName)) {
            IronSource.showRewardedVideo();
        } else {
            IronSource.showRewardedVideo(mRewardedVideoPlacementName);
        }
    }

    @Override
    public boolean isInitialized() {
        onLog("isInitialized: " + mIsInitialized);
        return mIsInitialized;
    }

    @Override
    public void onDestroy() {
        onLog("onDestroy");
    }

    @Override
    public void onPause() {
        onLog("onPause");
    }

    @Override
    public void onResume() {
        onLog("onResume");
    }

    /**
     * IronSource RewardedVideoListener implementation
     */

    @Override
    public void onRewardedVideoAvailabilityChanged(final boolean available) {
        onLog("onRewardedVideoAvailabilityChanged " + available);
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    if (available) {
                        onLog("onAdLoaded");
                        mMediationRewardedVideoAdListener.onAdLoaded(IronSourceRewardedAdapter.this);
                    } else {
                        onLog("onISAdFailedToLoad");
                        mMediationRewardedVideoAdListener.onAdFailedToLoad(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdOpened() {
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdOpened");
                    mMediationRewardedVideoAdListener.onAdOpened(IronSourceRewardedAdapter.this);
                    onLog("onVideoStarted");
                    mMediationRewardedVideoAdListener.onVideoStarted(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClosed() {
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClosed");
                    mMediationRewardedVideoAdListener.onAdClosed(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdStarted() {
        // Not called from IronSource SDK
    }

    @Override
    public void onRewardedVideoAdEnded() {
        // No relevant delegate in AdMob interface
    }

    @Override
    public void onRewardedVideoAdRewarded(final Placement placement) {
        if (placement == null) {
            onLog("IronSource Placement Error");
            return;
        }

        final IronSourceReward reward = new IronSourceReward(placement);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {

                    onLog("onRewarded: " + reward.getType() + " " + reward.getAmount());
                    mMediationRewardedVideoAdListener.onRewarded(IronSourceRewardedAdapter.this, new IronSourceReward(placement));
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(IronSourceError ironsourceError) {
        // No relevant delegate in AdMob interface
        onLog("onRewardedVideoAdShowFailed: " + ironsourceError.getErrorMessage());
    }

    @Override
    public void onRewardedVideoAdClicked(Placement placement) {
        onLog("onRewardedVideoAdClicked, placement: " + placement.getPlacementName());

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onAdClicked");
                    mMediationRewardedVideoAdListener.onAdClicked(IronSourceRewardedAdapter.this);
                    onLog("onAdLeftApplication");
                    mMediationRewardedVideoAdListener.onAdLeftApplication(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    /**
     * A {@link RewardItem} used to map IronSource reward to Google's reward.
     */

    class IronSourceReward implements RewardItem {

        private final Placement mPlacement;

        IronSourceReward(Placement placement) {
            this.mPlacement = placement;
        }

        @Override
        public String getType() {
            return mPlacement.getRewardName();
        }

        @Override
        public int getAmount() {
            return mPlacement.getRewardAmount();
        }
    }
}
