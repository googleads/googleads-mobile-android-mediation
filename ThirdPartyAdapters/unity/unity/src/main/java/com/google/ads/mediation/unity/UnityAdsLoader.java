package com.google.ads.mediation.unity;

import android.app.Activity;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;

/** Wrapper class for {@link UnitAds#load} and {@link UnityAds#show} */
class UnityAdsLoader {
  public void load(
      String placementId,
      UnityAdsLoadOptions unityAdsLoadOptions,
      IUnityAdsLoadListener unityAdsLoadListener) {
    UnityAds.load(placementId, unityAdsLoadOptions, unityAdsLoadListener);
  }

  public void show(
      Activity activity,
      String placementId,
      UnityAdsShowOptions unityAdsShowOptions,
      IUnityAdsShowListener unityAdsShowListener) {
    UnityAds.show(activity, placementId, unityAdsShowOptions, unityAdsShowListener);
  }

  public UnityAdsLoadOptions createUnityAdsLoadOptionsWithId(String objectId) {
    UnityAdsLoadOptions unityAdsLoadOptions = new UnityAdsLoadOptions();
    unityAdsLoadOptions.setObjectId(objectId);
    return unityAdsLoadOptions;
  }

  public UnityAdsShowOptions createUnityAdsShowOptionsWithId(String objectId) {
    UnityAdsShowOptions unityAdsShowOptions = new UnityAdsShowOptions();
    unityAdsShowOptions.setObjectId(objectId);
    return unityAdsShowOptions;
  }
}
