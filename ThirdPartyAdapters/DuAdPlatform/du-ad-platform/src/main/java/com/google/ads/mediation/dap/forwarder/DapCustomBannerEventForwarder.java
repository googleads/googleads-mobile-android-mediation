package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.banner.BannerListener;
import com.google.ads.mediation.dap.DuAdAdapter;
import com.google.ads.mediation.dap.DuBannerAdAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;


public class DapCustomBannerEventForwarder implements BannerListener {
    private static final String TAG = DuBannerAdAdapter.class.getSimpleName();
    private MediationBannerListener mBannerListener;
    private DuBannerAdAdapter mAdapter;

    public DapCustomBannerEventForwarder(DuBannerAdAdapter adAdapter, MediationBannerListener listener) {
        mBannerListener = listener;
        mAdapter = adAdapter;
    }

    @Override
    public void onAdLoaded() {
        mBannerListener.onAdLoaded(mAdapter);
    }

    @Override
    public void onError(String s) {
        DuAdAdapter.d(TAG, "onError - " + s);
        mBannerListener.onAdFailedToLoad(mAdapter, Integer.valueOf(s));
    }
}
