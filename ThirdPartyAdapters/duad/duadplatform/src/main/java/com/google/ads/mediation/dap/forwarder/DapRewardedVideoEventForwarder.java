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
                // These are invoked after the video ad ends as we currently don`t support ad click instant
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
            mRewardedVideoListener.onAdFailedToLoad(mMediationRewardedVideoAdAdapter, getAdMobErrorCode(adError.getErrorCode()));
        }
    }

    @Override
    public void onAdPlayable() {
        if (mRewardedVideoListener != null) {
            DuAdMediation.d(TAG, "Dap Rewarded Video loaded successfully, Forwarder instance: " + this);
            mRewardedVideoListener.onAdLoaded(mMediationRewardedVideoAdAdapter);
        }
    }

    private int getAdMobErrorCode(int duAdErrorCode){
        switch (duAdErrorCode){
            case 2000: // SERVER_ERROR_CODE: Server Error
            case 2001: // INTERNAL_ERROR_CODE: Network Error
            case 3001: // UNKNOWN_ERROR_CODE: Unknown Error
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
            case 1002: // LOAD_TOO_FREQUENTLY_ERROR_CODE: Too many interface requests
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case 1000: // NETWORK_ERROR_CODE: Client network error
            case 3000: // TIME_OUT_CODE: Retrieve Ad data timed out
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case 1001: // NO_FILL_ERROR_CODE: No Ad data retrieved
            case 1003: // IMPRESSION_LIMIT_ERROR_CODE: Reach the daily impression limit
                return AdRequest.ERROR_CODE_NO_FILL;
            default:
        }
        return AdRequest.ERROR_CODE_INTERNAL_ERROR;
    }
}
