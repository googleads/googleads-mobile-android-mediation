package com.google.ads.mediation.unity.eventadapters;

import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

public class UnityInterstitialEventAdapter implements IUnityEventAdapter {

  MediationInterstitialListener listener;
  MediationInterstitialAdapter adapter;

  public UnityInterstitialEventAdapter(MediationInterstitialListener listener,
      MediationInterstitialAdapter adapter) {
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
