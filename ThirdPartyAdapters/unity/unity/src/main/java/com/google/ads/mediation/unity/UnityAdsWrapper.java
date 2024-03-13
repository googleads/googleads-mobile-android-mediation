package com.google.ads.mediation.unity;

import android.content.Context;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

/** Wrapper class for {@link UnityAds#initialize} */
class UnityAdsWrapper {
  public void initialize(Context context, String gameId, IUnityAdsInitializationListener listener) {
    UnityAds.initialize(context, gameId, false, listener);
  }

  public boolean isInitialized() {
    return UnityAds.isInitialized();
  }

  public String getVersion() {
    return UnityAds.getVersion();
  }

  public MediationMetaData getMediationMetaData(Context context) {
    return new MediationMetaData(context);
  }
}
