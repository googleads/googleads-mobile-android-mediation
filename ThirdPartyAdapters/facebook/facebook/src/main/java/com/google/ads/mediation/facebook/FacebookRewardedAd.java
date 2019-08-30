package com.google.ads.mediation.facebook;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;

public class FacebookRewardedAd implements MediationRewardedAd, RewardedVideoAdExtendedListener {

    private MediationRewardedAdConfiguration adConfiguration;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;

    /**
     * Facebook rewarded video ad instance.
     */
    private RewardedVideoAd rewardedAd;

    /**
     * Mediation rewarded video ad listener used to forward rewarded video ad events from
     * the Facebook Audience Network SDK to the Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback mRewardedAdCallback;

    private boolean isRtbAd = false;
    private AtomicBoolean didRewardedAdClose = new AtomicBoolean();

    public FacebookRewardedAd(MediationRewardedAdConfiguration adConfiguration,
                              MediationAdLoadCallback<MediationRewardedAd,
                                      MediationRewardedAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.mMediationAdLoadCallback = callback;
    }

    public void render() {
        final Context context = adConfiguration.getContext();
        Bundle serverParameters = adConfiguration.getServerParameters();

        if (!FacebookMediationAdapter.isValidRequestParameters(context, serverParameters)) {
            mMediationAdLoadCallback.onFailure("Invalid request");
            return;
        }

        String decodedBid = adConfiguration.getBidResponse();
        if (!TextUtils.isEmpty(decodedBid)) {
            isRtbAd = true;
        }

        final String placementID = FacebookMediationAdapter.getPlacementID(serverParameters);
        if (isRtbAd) {
            rewardedAd = new RewardedVideoAd(context, placementID);
            rewardedAd.setAdListener(this);
            rewardedAd.loadAdFromBid(decodedBid);
        } else {
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
                            if (mMediationAdLoadCallback != null) {
                                mMediationAdLoadCallback.onFailure(logMessage);
                            }
                        }
                    });
        }
    }

    @Override
    public void showAd(Context context) {
        if (rewardedAd.isAdLoaded()) {
            rewardedAd.show();
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onVideoStart();
                mRewardedAdCallback.onAdOpened();
            }
        } else {
            if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow("No ads to show");
            }
        }
    }

    private void createAndLoadRewardedVideo(Context context, String placementID) {
        rewardedAd = new RewardedVideoAd(context, placementID);
        rewardedAd.setAdListener(this);
        rewardedAd.loadAd(true);
    }

    @Override
    public void onRewardedVideoCompleted() {
        mRewardedAdCallback.onVideoComplete();
        mRewardedAdCallback.onUserEarnedReward(new FacebookReward());
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
        rewardedAd.destroy();
    }

    @Override
    public void onAdLoaded(Ad ad) {
        if (mMediationAdLoadCallback != null) {
            mRewardedAdCallback = mMediationAdLoadCallback.onSuccess(this);
        }
    }

    @Override
    public void onAdClicked(Ad ad) {
        if (mRewardedAdCallback != null) {
            if (isRtbAd) {
                // TODO: Upon approval, add this callback back in.
                // mRewardedAdCallback.reportAdClicked();
            } else {
                mRewardedAdCallback.reportAdClicked();
            }
        }
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        if (mRewardedAdCallback != null) {
            if (isRtbAd) {
                // TODO: Upon approval, add this callback back in.
                // mRewardedAdCallback.reportAdImpression();
            } else {
                mRewardedAdCallback.reportAdImpression();
            }
        }
    }

    @Override
    public void onRewardedVideoClosed() {
        if (!didRewardedAdClose.getAndSet(true) && mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
        if (rewardedAd != null) {
            rewardedAd.destroy();
        }
    }

    @Override
    public void onRewardedVideoActivityDestroyed() {
        if (!didRewardedAdClose.getAndSet(true) && mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
        if (rewardedAd != null) {
            rewardedAd.destroy();
        }
    }
}
