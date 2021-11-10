package com.google.ads.mediation.unity.eventadapters;

public interface IUnityEventAdapter {
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
