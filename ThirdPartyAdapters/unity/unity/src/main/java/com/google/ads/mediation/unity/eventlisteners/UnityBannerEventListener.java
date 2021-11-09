package com.google.ads.mediation.unity.eventlisteners;

import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;

public class UnityBannerEventListener implements IUnityEventListener {

    MediationBannerListener listener;
    MediationBannerAdapter callingClass;

    public UnityBannerEventListener(MediationBannerListener listener, MediationBannerAdapter callingClass) {
        this.listener = listener;
        this.callingClass = callingClass;
    }

    @Override
    public void onAdLoaded() {
        listener.onAdLoaded(callingClass);
    }

    @Override
    public void onAdOpened() {
        listener.onAdOpened(callingClass);
    }

    @Override
    public void onAdClicked() {
        listener.onAdClicked(callingClass);
    }

    @Override
    public void onAdClosed() {
        listener.onAdClosed(callingClass);
    }

    @Override
    public void onAdLeftApplication() {
    listener.onAdLeftApplication(callingClass);
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