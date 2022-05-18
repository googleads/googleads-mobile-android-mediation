package com.google.ads.mediation.unity.eventadapters;

import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;

public class UnityBannerEventAdapter implements IUnityEventAdapter {

  MediationBannerListener listener;
  MediationBannerAdapter adapter;

  public UnityBannerEventAdapter(MediationBannerListener listener, MediationBannerAdapter adapter) {
    this.listener = listener;
    this.adapter = adapter;
  }

  @Override
  public void sendAdEvent(AdEvent adEvent) {
    if (listener == null) {
      return;
    }

    switch (adEvent) {
      case LOADED:
        listener.onAdLoaded(adapter);
        break;
      case OPENED:
        listener.onAdOpened(adapter);
        break;
      case CLICKED:
        listener.onAdClicked(adapter);
        break;
      case CLOSED:
        listener.onAdClosed(adapter);
        break;
      case LEFT_APPLICATION:
        listener.onAdLeftApplication(adapter);
        break;
      default:
        break;
    }
  }
}
