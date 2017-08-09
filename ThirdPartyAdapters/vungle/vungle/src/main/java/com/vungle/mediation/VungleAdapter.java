package com.vungle.mediation;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.vungle.publisher.AdConfig;

/**
 * A {@link MediationRewardedVideoAdAdapter} to load and show Vungle rewarded video ads using
 * Google Mobile Ads SDK mediation.
 */
public class VungleAdapter implements MediationRewardedVideoAdAdapter {

    /**
     * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
     */
    private class VungleReward implements RewardItem {
        private final String mType;
        private final int mAmount;

        public VungleReward(String type, int amount) {
            mType = type;
            mAmount = amount;
        }

        @Override
        public int getAmount() {
            return mAmount;
        }

        @Override
        public String getType() {
            return mType;
        }
    }

    private static final String TAG = VungleManager.class.getSimpleName();
    private final String mId = "rewardBased";
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private boolean mInitialized;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private String mPlacementForPlay;

    private final VungleListener mVungleListener = new VungleListener() {
        @Override
        public void onAdEnd(String placement,
                            final boolean wasSuccessfulView,
                            boolean wasCallToActionClicked) {
            if (mMediationRewardedVideoAdListener != null) {
                if (wasSuccessfulView) {
                    mMediationRewardedVideoAdListener.onRewarded(VungleAdapter.this,
                            new VungleReward("vungle", 1));
                }
                mMediationRewardedVideoAdListener.onAdClosed(VungleAdapter.this);
            }
        }

        @Override
        public void onAdStart(String placement) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdOpened(VungleAdapter.this);
                mMediationRewardedVideoAdListener.onVideoStarted(VungleAdapter.this);
            }
        }

        @Override
        public void onAdAvailable() {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdLoaded(VungleAdapter.this);
            }
        }

        @Override
        void onInitialized(boolean isSuccess) {
            if (mMediationRewardedVideoAdListener != null) {
                if (!isSuccess) {
                    mInitialized = false;
                    mMediationRewardedVideoAdListener.onInitializationFailed(
                            VungleAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                } else {
                    mInitialized = true;
                    mMediationRewardedVideoAdListener.onInitializationSucceeded(VungleAdapter.this);
                }
            }
        }

        @Override
        void onAdFail(String placement) {
            if (placement.equals(mPlacementForPlay)) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdClosed(VungleAdapter.this);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        mVungleManager.removeListener(mId);
        mInitialized = false;
    }

    @Override
    public void onPause() {
        mVungleManager.onPause();
    }

    @Override
    public void onResume() {
        mVungleManager.onResume();
    }

    @Override
    public void initialize(Context context, MediationAdRequest adRequest, String unused,
                           MediationRewardedVideoAdListener listener, Bundle serverParameters,
                           Bundle networkExtras) {
        String[] placements;
        if (networkExtras != null) {
            placements = networkExtras.getStringArray(VungleExtrasBuilder.EXTRA_ALL_PLACEMENTS);

            if (placements == null || placements.length == 0) {
                Log.e(TAG, "Placements should be specified!");
                if (listener != null) {
                    listener.onInitializationFailed(
                            VungleAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
                return;
            }
        } else {
            Log.e(TAG, "networkExtras is null.");
            if (listener != null) {
                listener.onInitializationFailed(
                        VungleAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationRewardedVideoAdListener = listener;
        String appId = serverParameters.getString("appid");
        mVungleManager = VungleManager.getInstance(appId, placements);
        mVungleManager.addListener(mId, mVungleListener);
        if (mVungleManager.isInitialized()) {
            mInitialized = true;
            mMediationRewardedVideoAdListener.onInitializationSucceeded(VungleAdapter.this);
        } else {
            mVungleListener.setWaitingInit(true);
            mVungleManager.init(context);
        }
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public void loadAd(MediationAdRequest adRequest, Bundle serverParameters,
                       Bundle networkExtras) {
        if (networkExtras != null) {
            mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(networkExtras);
            mPlacementForPlay = mVungleManager.findPlacemnt(networkExtras);
            if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdLoaded(VungleAdapter.this);
                }
            } else {
                mVungleListener.waitForAd(mPlacementForPlay);
                mVungleManager.loadAd(mPlacementForPlay);
            }
        } else {
            Log.e(TAG, "'playPlacement' should be specified!");
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdFailedToLoad(
                        VungleAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    @Override
    public void showVideo() {
        mVungleManager.playAd(mPlacementForPlay, mAdConfig, mId);
    }
}
