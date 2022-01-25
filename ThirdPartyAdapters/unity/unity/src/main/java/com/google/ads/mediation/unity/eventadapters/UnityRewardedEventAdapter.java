package com.google.ads.mediation.unity.eventadapters;

import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import com.google.ads.mediation.unity.UnityReward;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;

public class UnityRewardedEventAdapter implements IUnityEventAdapter {

  MediationRewardedAdCallback listener;

  public UnityRewardedEventAdapter(MediationRewardedAdCallback listener) {
    this.listener = listener;
  }

  public void sendAdEvent(AdEvent adEvent) {
    if (listener == null) {
      return;
    }

    switch (adEvent) {
      case OPENED:
        listener.onAdOpened();
        break;
      case CLICKED:
        listener.reportAdClicked();
        break;
      case CLOSED:
        listener.onAdClosed();
        break;
      case IMPRESSION:
        listener.reportAdImpression();
        break;
      case VIDEO_START:
        listener.onVideoStart();
        break;
      case REWARD:
        // Unity Ads doesn't provide a reward value. The publisher is expected to
        // override the reward in AdMob console.
        listener.onUserEarnedReward(new UnityReward());
        break;
      case VIDEO_COMPLETE:
        listener.onVideoComplete();
        break;
      default:
        break;
    }
  }
}
