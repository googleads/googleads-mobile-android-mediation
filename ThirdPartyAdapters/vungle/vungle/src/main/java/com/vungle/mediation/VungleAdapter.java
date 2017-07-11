package com.vungle.mediation;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.vungle.publisher.AdConfig;

public class VungleAdapter implements MediationRewardedVideoAdAdapter {

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
    private final String ID = "rewardBased";
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private boolean mInitialized;
    private VungleManager mVunglePub;
    private AdConfig adConfig;
    private String placementForPlay;

    private final VungleListener vungleListener = new VungleListener() {
        @Override
        public void onAdEnd(String placement, final boolean wasSuccessfulView, boolean wasCallToActionClicked) {
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
                if (!isSuccess){
                    mInitialized = false;
                    mMediationRewardedVideoAdListener.onInitializationFailed(VungleAdapter.this, 0);
                } else {
                    mInitialized = true;
                    mMediationRewardedVideoAdListener.onInitializationSucceeded(VungleAdapter.this);
                }
            }
        }

        @Override
        void onAdFail(String placement) {
            if (placement.equals(placementForPlay)) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdClosed(VungleAdapter.this);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        mVunglePub.removeListener(ID);
        mInitialized = false;
    }

    @Override
    public void onPause() {
        mVunglePub.onPause();
    }

    @Override
    public void onResume() {
        mVunglePub.onResume();
    }

    @Override
    public void initialize(Context context, MediationAdRequest adRequest, String unused,
                           MediationRewardedVideoAdListener listener, Bundle serverParameters,
                           Bundle networkExtras) {
        String[] placements = networkExtras.getStringArray(VungleExtrasBuilder.EXTRA_ALL_PLACEMENTS);

        if (placements == null || placements.length == 0) {
            Log.e(TAG, "Placements should be specified!");
            if (listener != null)
                listener.onInitializationFailed(VungleAdapter.this, 0);
            return;
        }
        mMediationRewardedVideoAdListener = listener;
        String appId = serverParameters.getString("appid");
        mVunglePub = VungleManager.getInstance(appId, placements, context);
        mVunglePub.addListener(ID, vungleListener);
        if (mVunglePub.isInitialized()) {
            mInitialized = true;
            mMediationRewardedVideoAdListener.onInitializationSucceeded(VungleAdapter.this);
        } else {
            vungleListener.setWaitingInit(true);
            mVunglePub.init();
        }
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    public void loadAd(MediationAdRequest adRequest, Bundle serverParameters,
                       Bundle networkExtras) {
        adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(networkExtras);
        placementForPlay = mVunglePub.findPlacemnt(networkExtras);
        if (mVunglePub.isAdPlayable(placementForPlay)) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdLoaded(VungleAdapter.this);
            }
        } else {
            vungleListener.waitForAd(placementForPlay);
            mVunglePub.loadAd(placementForPlay);
        }
    }

    @Override
    public void showVideo() {
        mVunglePub.playAd(placementForPlay, adConfig, ID);
    }
}
