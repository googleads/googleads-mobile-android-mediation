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

import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;

/**
 * A {@link MediationRewardedVideoAdAdapter} used to mediate rewarded video ads from IronSource.
 */
public class IronSourceRewardedAdapter extends IronSourceBaseAdapter
        implements MediationRewardedVideoAdAdapter, ISDemandOnlyRewardedVideoListener {

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    private static boolean mDidInitRewardedVideo = false;
    private static boolean mDidReceiveFirstAvailability = false;

    //region MediationRewardedVideoAdAdapter implementation.
    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {

        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;

        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            onLog("IronSource SDK requires an Activity context to initialize");
            mediationRewardedVideoAdListener.onInitializationFailed(
                    IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        try {
            // Parse enabling testing mode key for log.
            this.mIsLogEnabled = mediationAdRequest.isTesting();

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("IronSource initialization failed, make sure that 'appKey' server parameter"
                        + " is added");
                mediationRewardedVideoAdListener.onInitializationFailed(
                        IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);

                return;
            }

            this.mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, "0");

            IronSource.setISDemandOnlyRewardedVideoListener(this);
            if (!mDidInitRewardedVideo) {
                this.mDidInitRewardedVideo = true;
                onLog("IronSource initialization succeeded for RewardedVideo");
                initIronSourceSDK(context, appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            }

            mediationRewardedVideoAdListener.
                    onInitializationSucceeded(IronSourceRewardedAdapter.this);

        } catch (Exception e) {
            mediationRewardedVideoAdListener.onInitializationFailed(
                    IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
        this.mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, "0");

        if (mDidReceiveFirstAvailability) {
            if (IronSource.isISDemandOnlyRewardedVideoAvailable(this.mInstanceID)) {
                mMediationRewardedVideoAdListener.onAdLoaded(IronSourceRewardedAdapter.this);
            } else {
                mMediationRewardedVideoAdListener.onAdFailedToLoad(
                        IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            }
        } else {
            // We are waiting for a response from IronSource SDK
            // (see: 'onRewardedVideoAvailabilityChanged'). Once we retrieved a response we notify
            // AdMob for availability. From then on for every other load we update AdMob with
            // IronSource rewarded video current availability (see: 'hasVideoAvailable').
        }
    }

    @Override
    public void showVideo() {

        if (IronSource.isISDemandOnlyRewardedVideoAvailable(this.mInstanceID)) {
            IronSource.showISDemandOnlyRewardedVideo(this.mInstanceID);
        } else {
            // Show ad will only be called if the adapter sends back an ad
            // loaded callback in response to a loadAd request. If for any
            // reason the adapter is not ready to show an ad after sending
            // an ad loaded callback, log a warning.
            onLog("No ads to show.");
        }
    }

    @Override
    public boolean isInitialized() {
        return this.mDidInitRewardedVideo;
    }
    //endregion

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }


    /**
     * IronSource RewardedVideoListener implementation.
     */
    @Override
    public void onRewardedVideoAvailabilityChanged(final String instanceId,
                                                   final boolean available) {
        onLog("IronSource Rewarded Video changed availability: " + available + " for instance "
                + instanceId);

        // We handle callbacks only for registered instances.
        if (!this.mInstanceID.equals(instanceId))
            return;

        if (!mDidReceiveFirstAvailability) {
            mDidReceiveFirstAvailability = true;
            if (mMediationRewardedVideoAdListener != null) {
                sendEventOnUIThread(new Runnable() {
                    public void run() {
                        if (available) {
                            mMediationRewardedVideoAdListener.
                                    onAdLoaded(IronSourceRewardedAdapter.this);
                            onLog("IronSource Rewarded Video loaded successfully for instance "
                                    + instanceId);

                        } else {
                            mMediationRewardedVideoAdListener.onAdFailedToLoad(
                                    IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                            onLog("IronSource Rewarded Video failed to load for instance "
                                    + instanceId);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onRewardedVideoAdOpened(final String instanceId) {
        onLog("IronSource Rewarded Video opened ad for instance " + instanceId);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdOpened(IronSourceRewardedAdapter.this);
                    mMediationRewardedVideoAdListener.
                            onVideoStarted(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        onLog("IronSource Rewarded Video closed ad for instance " + instanceId);
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdClosed(IronSourceRewardedAdapter.this);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String instanceId, final Placement placement) {

        if (placement == null) {
            onLog("IronSource Placement Error");
            return;
        }

        final IronSourceReward reward = new IronSourceReward(placement);
        onLog("IronSource Rewarded Video received reward " + reward.getType() + " "
                + reward.getAmount() + ", for instance: " + instanceId);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onRewarded(
                            IronSourceRewardedAdapter.this, new IronSourceReward(placement));
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironsourceError) {
        // No relevant delegate in AdMob interface.
        onLog("IronSource Rewarded Video failed to show for instance " + instanceId);
    }

    @Override
    public void onRewardedVideoAdClicked(String instanceId, Placement placement) {
        onLog("IronSource Rewarded Video clicked for instance " + instanceId);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdClicked(IronSourceRewardedAdapter.this);
                    mMediationRewardedVideoAdListener.
                            onAdLeftApplication(IronSourceRewardedAdapter.this);
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
