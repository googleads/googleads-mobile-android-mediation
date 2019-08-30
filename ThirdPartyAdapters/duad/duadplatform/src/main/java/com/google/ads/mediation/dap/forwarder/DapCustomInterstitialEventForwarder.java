package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.InterstitialListener;
import com.google.ads.mediation.dap.DuAdMediation;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

public class DapCustomInterstitialEventForwarder implements InterstitialListener {

    private static final String TAG = DapCustomInterstitialEventForwarder.class.getSimpleName();
    private final MediationInterstitialAdapter mAdapter;
    private MediationInterstitialListener mInterstitialListener;

    public DapCustomInterstitialEventForwarder(MediationInterstitialAdapter adapter,
                                               MediationInterstitialListener listener) {
        mInterstitialListener = listener;
        mAdapter = adapter;
    }

    @Override
    public void onAdFail(int i) {
        if (mInterstitialListener != null) {
            DuAdMediation.debugLog(TAG, "Interstitial onAdFail -  " + i);
            mInterstitialListener.onAdFailedToLoad(mAdapter, getAdMobErrorCode(i));
        }
    }

    @Override
    public void onAdReceive() {
        if (mInterstitialListener != null) {
            DuAdMediation.debugLog(TAG, "Interstitial onAdReceive ");
            mInterstitialListener.onAdLoaded(mAdapter);
        }
    }

    @Override
    public void onAdDismissed() {
        if (mInterstitialListener != null) {
            DuAdMediation.debugLog(TAG, "Interstitial onAdDismissed ");
            mInterstitialListener.onAdClosed(mAdapter);
        }
    }

    @Override
    public void onAdPresent() {
        if (mInterstitialListener != null) {
            DuAdMediation.debugLog(TAG, "Interstitial onAdPresent ");
            mInterstitialListener.onAdOpened(mAdapter);
        }
    }

    @Override
    public void onAdClicked() {
        if (mInterstitialListener != null) {
            DuAdMediation.debugLog(TAG, "Interstitial onAdClicked ");
            mInterstitialListener.onAdClicked(mAdapter);
            mInterstitialListener.onAdLeftApplication(mAdapter);
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
