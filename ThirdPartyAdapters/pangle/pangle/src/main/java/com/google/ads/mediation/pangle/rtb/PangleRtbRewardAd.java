package com.google.ads.mediation.pangle.rtb;


import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SHOW_AD_NOT_LOADED;
import static com.google.ads.mediation.pangle.PangleConstant.PANGLE_SDK_ERROR_DOMAIN;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class PangleRtbRewardAd implements MediationRewardedAd {

    private static final String TAG = PangleRtbRewardAd.class.getSimpleName();
    private final MediationRewardedAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback;
    private MediationRewardedAdCallback rewardAdCallback;
    private TTRewardVideoAd ttRewardVideoAd;

    public PangleRtbRewardAd(@NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                             @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
        adConfiguration = mediationRewardedAdConfiguration;
        adLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        PangleMediationAdapter.setCoppa(adConfiguration);
        String placementId = adConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementId)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = adConfiguration.getBidResponse();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(adConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadRewardVideoAd(adSlot, new TTAdNative.RewardVideoAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                if (adLoadCallback != null) {
                    adLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
                }
            }

            @Override
            public void onRewardVideoAdLoad(TTRewardVideoAd ttRewardVideoAd) {
                rewardAdCallback = adLoadCallback.onSuccess(PangleRtbRewardAd.this);
                PangleRtbRewardAd.this.ttRewardVideoAd = ttRewardVideoAd;
            }

            @Override
            public void onRewardVideoCached() {

            }
        });
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (ttRewardVideoAd == null){
            rewardAdCallback.onAdFailedToShow(new AdError(ERROR_SHOW_AD_NOT_LOADED, "reward ad object is null", PANGLE_SDK_ERROR_DOMAIN));
        }
        ttRewardVideoAd.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
            @Override
            public void onAdShow() {
                rewardAdCallback.onAdOpened();
                rewardAdCallback.reportAdImpression();
            }

            @Override
            public void onAdVideoBarClick() {
                if (rewardAdCallback != null) {
                    rewardAdCallback.reportAdClicked();
                }
            }

            @Override
            public void onAdClose() {
                if (rewardAdCallback != null) {
                    rewardAdCallback.onAdClosed();
                }
            }

            @Override
            public void onVideoComplete() {

            }

            @Override
            public void onVideoError() {

            }

            @Override
            public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName, int errorCode, String errorMsg) {
                if (!rewardVerify) {
                    Log.d(TAG, "onRewardVerify(): "
                            + rewardVerify + ", errorCode = " + errorCode + ", errorMsg = " + errorMsg);
                    return;
                }
                final String rewardType = rewardName;
                final int amount = rewardAmount;

                RewardItem rewardItem = new RewardItem() {
                    @NonNull
                    @Override
                    public String getType() {
                        return rewardType;
                    }

                    @Override
                    public int getAmount() {
                        return amount;
                    }
                };
                if (rewardAdCallback != null) {
                    rewardAdCallback.onUserEarnedReward(rewardItem);
                }
            }

            @Override
            public void onSkippedVideo() {

            }
        });
        if (context instanceof Activity) {
            ttRewardVideoAd.showRewardVideoAd((Activity) context);
        } else {
            ttRewardVideoAd.showRewardVideoAd(null);
        }
    }
}
