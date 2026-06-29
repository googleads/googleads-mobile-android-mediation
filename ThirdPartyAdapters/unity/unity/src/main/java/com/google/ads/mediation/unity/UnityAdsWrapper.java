package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationInfo;

import com.unity3d.ads.IUnityAdsTokenListener;
import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.TokenConfiguration;
import com.unity3d.ads.UnityAds;

/** Wrapper class for {@link UnityAds} */
class UnityAdsWrapper {
  public void initialize(String gameId, InitializationListener listener) {
    // Build InitializationConfiguration with mediation info
    InitializationConfiguration config = new InitializationConfiguration.Builder(gameId)
        .withTestMode(false)
        .withMediationInfo(getMediationInfo())
        .build();

    UnityAds.initialize(config, listener);
  }

  public boolean isInitialized() {
    return UnityAds.isInitialized();
  }

  public String getVersion() {
    return UnityAds.getVersion();
  }

  public void getToken(IUnityAdsTokenListener tokenListener) {
    UnityAds.getToken(tokenListener);
  }

  public void getToken(
      TokenConfiguration tokenConfiguration, IUnityAdsTokenListener tokenListener) {
    UnityAds.getToken(tokenConfiguration, tokenListener);
  }
}
