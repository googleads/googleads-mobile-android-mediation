package com.google.ads.mediation.unity;

import android.content.Context;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsTokenListener;
import com.unity3d.ads.TokenConfiguration;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

/** Wrapper class for {@link UnityAds} */
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

  public void getToken(IUnityAdsTokenListener tokenListener) {
    UnityAds.getToken(tokenListener);
  }

  public void getToken(TokenConfiguration tokenConfiguration, IUnityAdsTokenListener tokenListener) {
    UnityAds.getToken(tokenConfiguration, tokenListener);
  }

}
