package com.google.ads.mediation.unity;

import android.util.Log;

import com.google.ads.mediation.unity.eventlisteners.IUnityEventListener;

public class UnityEventSender {

    static final String TAG = UnityMediationAdapter.class.getSimpleName();

    IUnityEventListener eventListener;

    UnityEventSender(IUnityEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void sendAdEvent(UnityAdsAdapterUtils.AdEvent adEvent) {
        if (eventListener == null) {
            return;
        }

        switch (adEvent) {
            case LOADED:
                eventListener.onAdLoaded();
                break;
            case OPEN:
                eventListener.onAdOpened();
                break;
            case IMPRESSION:
                eventListener.reportAdImpression();
                break;
            case VIDEO_START:
                eventListener.onVideoStart();
                break;
            case CLICK:
                eventListener.onAdClicked();
                break;
            case LEFT_APPLICATION:
                eventListener.onAdLeftApplication();
                break;
            case REWARD:
                eventListener.onUserEarnedReward();
                break;
            case COMPLETE:
                eventListener.onVideoComplete();
                break;
            case CLOSE:
                eventListener.onAdClosed();
                break;
            default:
                Log.e(TAG, "Unknown ad event");
                break;
        }
    }


}
