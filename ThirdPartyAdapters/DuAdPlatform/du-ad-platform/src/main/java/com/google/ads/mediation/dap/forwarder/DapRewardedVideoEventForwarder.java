package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.AdError;
import com.duapps.ad.video.AdResult;
import com.duapps.ad.video.DuVideoAdListener;
import com.google.ads.mediation.dap.DuAdAdapter;
import com.google.ads.mediation.dap.DuRewardedAdAdapter;
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
                mRewardedVideoListener.onAdClicked(mMediationRewardedVideoAdAdapter);
                DuAdAdapter.d(TAG, "Dap Rewarded Video clicked.");
            }
            mRewardedVideoListener.onAdClosed(mMediationRewardedVideoAdAdapter);
            DuAdAdapter.d(TAG, "Dap Rewarded Video closed.");
        }
    }

    @Override
    public void onAdStart() {
        if (mRewardedVideoListener != null) {
            DuAdAdapter.d(TAG, "Dap Rewarded Video started playing.");
            mRewardedVideoListener.onAdOpened(mMediationRewardedVideoAdAdapter);
            mRewardedVideoListener.onVideoStarted(mMediationRewardedVideoAdAdapter);
        }
    }

    @Override
    public void onAdError(AdError adError) {
        if (mRewardedVideoListener != null) {
            DuAdAdapter.d(TAG, "Loading/Playing Dap Rewarded Video encountered an error: " + adError.getErrorCode());

            mRewardedVideoListener.onAdFailedToLoad(mMediationRewardedVideoAdAdapter, adError.getErrorCode());
        }
    }

    @Override
    public void onAdPlayable() {
        if (mRewardedVideoListener != null) {
            DuAdAdapter.d(TAG, "Dap Rewarded Video loaded successfully.");
            mRewardedVideoListener.onAdLoaded(mMediationRewardedVideoAdAdapter);
        }
    }
}
