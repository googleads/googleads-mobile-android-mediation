package com.google.ads.mediation.unity.eventlisteners;

public interface IUnityEventListener {
        void onAdLoaded();
        void onAdOpened();
        void onAdClicked();
        void onAdClosed();
        void onAdLeftApplication();
        void reportAdImpression();
        void onVideoStart();
        void onUserEarnedReward();
        void onVideoComplete();
}
