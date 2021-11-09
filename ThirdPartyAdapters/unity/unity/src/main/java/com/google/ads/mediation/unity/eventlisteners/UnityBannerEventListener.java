package com.google.ads.mediation.unity.eventlisteners;

import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;

public class UnityBannerEventListener implements IUnityEventListener {

    MediationBannerListener listener;
    MediationBannerAdapter adapter;

    public UnityBannerEventListener(MediationBannerListener listener, MediationBannerAdapter adapter) {
        this.listener = listener;
        this.adapter = adapter;
    }

    @Override
    public void onAdLoaded() {
        listener.onAdLoaded(adapter);
    }

    @Override
    public void onAdOpened() {
        listener.onAdOpened(adapter);
    }

    @Override
    public void onAdClicked() {
        listener.onAdClicked(adapter);
    }

    @Override
    public void onAdClosed() {
        listener.onAdClosed(adapter);
    }

    @Override
    public void onAdLeftApplication() {
    listener.onAdLeftApplication(adapter);
    }

    @Override
    public void reportAdImpression() {
        // no-op
    }

    @Override
    public void onVideoStart() {
        // no-op
    }

    @Override
    public void onUserEarnedReward() {
        // no-op
    }

    @Override
    public void onVideoComplete() {
        // no-op
    }
}