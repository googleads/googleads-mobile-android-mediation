package com.google.ads.mediation.unity.eventadapters;

import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;

public interface IUnityEventAdapter {

  void sendAdEvent(AdEvent adEvent);
}
