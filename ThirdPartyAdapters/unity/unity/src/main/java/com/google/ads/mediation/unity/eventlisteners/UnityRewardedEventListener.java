package com.google.ads.mediation.unity.eventlisteners;

import com.google.ads.mediation.unity.UnityReward;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;

public class UnityRewardedEventListener implements IUnityEventListener {

    MediationRewardedAdCallback listener;

    public UnityRewardedEventListener(MediationRewardedAdCallback listener) {
        this.listener = listener;
    }

    @Override
    public void onAdLoaded() {
        // no-op
    }

    @Override
    public void onAdOpened() {
        listener.onAdOpened();
    }

    @Override
    public void onAdClicked() {
        listener.reportAdClicked();
    }

    @Override
    public void onAdClosed() {
        listener.onAdClosed();
    }

    @Override
    public void onAdLeftApplication() {
        // no-op
    }

    @Override
    public void reportAdImpression() {
        listener.reportAdImpression();
    }

    @Override
    public void onVideoStart() {
        listener.onVideoStart();
    }

    @Override
    public void onUserEarnedReward() {
        // Unity Ads doesn't provide a reward value. The publisher is expected to
        // override the reward in AdMob console.
        listener.onUserEarnedReward(new UnityReward());
    }

    @Override
    public void onVideoComplete() {
        listener.onVideoComplete();
    }
}
