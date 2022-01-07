package com.google.ads.mediation.pangle.rtb;


import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;
import static com.google.ads.mediation.pangle.PangleConstant.ERROR_SHOW_FAIL;
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

public class PangleRtbRewardAdapter implements MediationRewardedAd {
    private static final String TAG = "PangleRtbRewardAdapter";

    private final MediationRewardedAdConfiguration mAdConfiguration;
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
    private MediationRewardedAdCallback mRewardAdCallback;
    private TTRewardVideoAd mTTRewardVideoAd;

    public PangleRtbRewardAdapter(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                                  MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
        mAdConfiguration = mediationRewardedAdConfiguration;
        mAdLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        PangleMediationAdapter.setCoppa(mAdConfiguration);
        String placementId = mAdConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementId)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            mAdLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = mAdConfiguration.getBidResponse();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(mAdConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementId)
                .setImageAcceptedSize(1080, 1920) //Set size to fit your ad slot size
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadRewardVideoAd(adSlot, new TTAdNative.RewardVideoAdListener() {
            @Override
            public void onError(int errorCode, String errorMessage) {
                if (mAdLoadCallback != null) {
                    mAdLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, errorMessage));
                }
            }

            @Override
            public void onRewardVideoAdLoad(TTRewardVideoAd ttRewardVideoAd) {
                mRewardAdCallback = mAdLoadCallback.onSuccess(PangleRtbRewardAdapter.this);
                mTTRewardVideoAd = ttRewardVideoAd;
            }

            @Override
            public void onRewardVideoCached() {

            }
        });
    }

    @Override
    public void showAd(@NonNull Context context) {
        try {
            if (mTTRewardVideoAd != null) {
                mTTRewardVideoAd.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {
                    @Override
                    public void onAdShow() {
                        mRewardAdCallback.onAdOpened();
                        mRewardAdCallback.reportAdImpression();
                    }

                    @Override
                    public void onAdVideoBarClick() {
                        if (mRewardAdCallback != null) {
                            mRewardAdCallback.reportAdClicked();
                        }
                    }

                    @Override
                    public void onAdClose() {
                        if (mRewardAdCallback != null) {
                            mRewardAdCallback.onAdClosed();
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
                        if (rewardVerify) {
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
                            if (mRewardAdCallback != null) {
                                mRewardAdCallback.onUserEarnedReward(rewardItem);
                            }
                        } else {
                            Log.d(TAG, "onRewardVerify(): "
                                    + rewardVerify + ", errorCode = " + errorCode + ", errorMsg = " + errorMsg);
                        }
                    }

                    @Override
                    public void onSkippedVideo() {

                    }
                });
                if (context instanceof Activity) {
                    mTTRewardVideoAd.showRewardVideoAd((Activity) context);
                } else {
                    mTTRewardVideoAd.showRewardVideoAd(null);
                }
            } else {
                if (mRewardAdCallback != null) {
                    mRewardAdCallback.onAdFailedToShow(new AdError(ERROR_SHOW_FAIL, "reward ad object is null", PANGLE_SDK_ERROR_DOMAIN));
                } else {
                    mAdLoadCallback.onFailure(new AdError(ERROR_SHOW_FAIL, "reward ad object is null", PANGLE_SDK_ERROR_DOMAIN));
                }
            }
        } catch (Throwable ignore) {
            if (mRewardAdCallback != null) {
                mRewardAdCallback.onAdFailedToShow(new AdError(ERROR_SHOW_FAIL, "reward failed to show", PANGLE_SDK_ERROR_DOMAIN));
            } else {
                mAdLoadCallback.onFailure(new AdError(ERROR_SHOW_FAIL, "reward failed to show", PANGLE_SDK_ERROR_DOMAIN));
            }
        }
    }
}
