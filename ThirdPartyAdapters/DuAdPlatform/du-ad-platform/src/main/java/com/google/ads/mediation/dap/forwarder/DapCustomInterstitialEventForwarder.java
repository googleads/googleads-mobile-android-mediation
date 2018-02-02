package com.google.ads.mediation.dap.forwarder;


import com.duapps.ad.InterstitialListener;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

public class DapCustomInterstitialEventForwarder implements InterstitialListener {

    private static final String TAG = DapCustomInterstitialEventForwarder.class.getSimpleName();
    private final MediationInterstitialAdapter mAdapter;
    private MediationInterstitialListener mInterstitialListener;

    public DapCustomInterstitialEventForwarder(MediationInterstitialAdapter adapter, MediationInterstitialListener listener) {
        mInterstitialListener = listener;
        mAdapter = adapter;
    }

    @Override
    public void onAdFail(int i) {
        if (mInterstitialListener != null) {
            DuAdMediation.d(TAG, "Interstitial onAdFail -  " + i);
            mInterstitialListener.onAdFailedToLoad(mAdapter, i);
        }
    }

    @Override
    public void onAdReceive() {
        if (mInterstitialListener != null) {
            DuAdMediation.d(TAG, "Interstitial onAdReceive ");
            mInterstitialListener.onAdLoaded(mAdapter);
        }
    }

    @Override
    public void onAdDismissed() {
        if (mInterstitialListener != null) {
            DuAdMediation.d(TAG, "Interstitial onAdDismissed ");
            mInterstitialListener.onAdClosed(mAdapter);
        }
    }

    @Override
    public void onAdPresent() {
        if (mInterstitialListener != null) {
            DuAdMediation.d(TAG, "Interstitial onAdPresent ");
            mInterstitialListener.onAdOpened(mAdapter);
        }
    }

    @Override
    public void onAdClicked() {
        if (mInterstitialListener != null) {
            DuAdMediation.d(TAG, "Interstitial onAdClicked ");
            mInterstitialListener.onAdClicked(mAdapter);
        }

    }
}
