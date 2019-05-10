package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.banner.BannerListener;
import com.google.ads.mediation.dap.DuAdAdapter;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationBannerListener;

public class DapCustomBannerEventForwarder implements BannerListener {

    private static final String TAG = DuAdAdapter.class.getSimpleName();
    private MediationBannerListener mBannerListener;
    private DuAdAdapter mAdapter;

    public DapCustomBannerEventForwarder(DuAdAdapter adAdapter, MediationBannerListener listener) {
        mBannerListener = listener;
        mAdapter = adAdapter;
    }

    @Override
    public void onAdLoaded() {
        DuAdMediation.debugLog(TAG, "Banner Ad Loaded");
        DuAdMediation.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mBannerListener.onAdLoaded(mAdapter);
            }
        });
    }

    @Override
    public void onError(String s) {
        DuAdMediation.debugLog(TAG, "Banner Ad onError - msg: " + s);
        DuAdMediation.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mBannerListener.onAdFailedToLoad(mAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        });
    }
}
