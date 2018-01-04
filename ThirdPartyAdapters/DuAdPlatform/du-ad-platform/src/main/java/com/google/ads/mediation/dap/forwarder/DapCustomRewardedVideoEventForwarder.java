package com.google.ads.mediation.dap.forwarder;

import android.util.Log;

import com.duapps.ad.AdError;
import com.duapps.ad.video.AdResult;
import com.duapps.ad.video.DuVideoAdListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

public class DapCustomRewardedVideoEventForwarder implements DuVideoAdListener {
    private static final String TAG = "DapRewardedVideoCustom";

    private final MediationRewardedVideoAdListener mRewardedVideoListener;
    private final MediationRewardedVideoAdAdapter mMediationRewardedVideoAdAdapter;

    public DapCustomRewardedVideoEventForwarder(
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
                Log.d(TAG, "Dap Rewarded Video clicked.");
            }
            mRewardedVideoListener.onAdClosed(mMediationRewardedVideoAdAdapter);
            Log.d(TAG, "Dap Rewarded Video closed.");
        }
    }

    @Override
    public void onAdStart() {
        if (mRewardedVideoListener != null) {
            Log.d(TAG, "Dap Rewarded Video started playing.");
            mRewardedVideoListener.onAdOpened(mMediationRewardedVideoAdAdapter);
            mRewardedVideoListener.onVideoStarted(mMediationRewardedVideoAdAdapter);
        }
    }

    @Override
    public void onAdError(AdError adError) {
        if (mRewardedVideoListener != null) {
            Log.d(TAG, "Loading/Playing Dap Rewarded Video encountered an error: " + adError.getErrorCode());

            mRewardedVideoListener.onAdFailedToLoad(mMediationRewardedVideoAdAdapter, adError.getErrorCode());
        }
    }

    @Override
    public void onAdPlayable() {
        if (mRewardedVideoListener != null) {
            Log.d(TAG, "Dap Rewarded Video loaded successfully.");
            mRewardedVideoListener.onAdLoaded(mMediationRewardedVideoAdAdapter);
        }
    }
}
