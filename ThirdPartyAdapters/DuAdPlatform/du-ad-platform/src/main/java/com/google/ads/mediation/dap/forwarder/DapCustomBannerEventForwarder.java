package com.google.ads.mediation.dap.forwarder;

import android.util.Log;

import com.duapps.ad.banner.BannerAdView;
import com.duapps.ad.banner.BannerListener;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;


public class DapCustomBannerEventForwarder implements BannerListener {
    private static final String TAG = "DAP mediation - Banner";
    private CustomEventBannerListener mBannerListener;
    private BannerAdView mAdView;

    public DapCustomBannerEventForwarder(CustomEventBannerListener listener, BannerAdView adView) {
        mBannerListener = listener;
        mAdView = adView;
    }

    @Override
    public void onAdLoaded() {
        Log.d(TAG, " onAdLoaded , mAdView = " + mAdView);


        mBannerListener.onAdLoaded(mAdView);

    }

    @Override
    public void onError(String s) {
        Log.d(TAG, "onError - " + s);

        mBannerListener.onAdFailedToLoad(Integer.valueOf(s));
    }
}
