package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.AdError;
import com.duapps.ad.video.AdResult;
import com.duapps.ad.video.DuVideoAdListener;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.ads.mediation.dap.DuRewardedAdAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

public class DapRewardedVideoEventForwarder implements DuVideoAdListener {
    private static final String TAG = DuRewardedAdAdapter.class.getSimpleName();

    private final MediationRewardedVideoAdListener mRewardedVideoListener;
    private final MediationRewardedVideoAdAdapter mMediationRewardedVideoAdAdapter;

    public DapRewardedVideoEventForwarder(
            MediationRewardedVideoAdAdapter adapter,
            MediationRewardedVideoAdListener listener) {
        mRewardedVideoListener = listener;
        mMediationRewardedVideoAdAdapter = adapter;
    }

    @Override
    public void onAdEnd(AdResult adResult) {
        if (mRewardedVideoListener != null) {
            if (adResult.isCallToActionClicked()) {
                // these are invoked after the video ad is ended as we currently don`t support ad click instant
                // callback. We will support it in future if the our publishers do have such need.
                mRewardedVideoListener.onAdClicked(mMediationRewardedVideoAdAdapter);
                mRewardedVideoListener.onAdLeftApplication(mMediationRewardedVideoAdAdapter);
                DuAdMediation.d(TAG, "Dap Rewarded Video clicked.");
            }
            if (adResult.isSuccessfulView()) {
                mRewardedVideoListener.onRewarded(mMediationRewardedVideoAdAdapter, null);
            }
            mRewardedVideoListener.onAdClosed(mMediationRewardedVideoAdAdapter);
            DuAdMediation.d(TAG, "Dap Rewarded Video closed.");
        }
    }

    @Override
    public void onAdStart() {
        if (mRewardedVideoListener != null) {
            DuAdMediation.d(TAG, "Dap Rewarded Video started playing.");
            mRewardedVideoListener.onAdOpened(mMediationRewardedVideoAdAdapter);
            mRewardedVideoListener.onVideoStarted(mMediationRewardedVideoAdAdapter);
        }
    }

    @Override
    public void onAdError(AdError adError) {
        if (mRewardedVideoListener != null) {
            DuAdMediation.d(TAG, "Loading/Playing Dap Rewarded Video encountered an error: " + adError.getErrorCode());
            mRewardedVideoListener.onAdFailedToLoad(mMediationRewardedVideoAdAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }

    @Override
    public void onAdPlayable() {
        if (mRewardedVideoListener != null) {
            DuAdMediation.d(TAG, "Dap Rewarded Video loaded successfully, Forwarder instance: " + this);
            mRewardedVideoListener.onAdLoaded(mMediationRewardedVideoAdAdapter);
        }
    }
}
