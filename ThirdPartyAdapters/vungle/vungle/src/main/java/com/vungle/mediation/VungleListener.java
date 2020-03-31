package com.vungle.mediation;

/**
 * A listener class used to send Vungle events from {@link VungleManager} to {@link
 * VungleInterstitialAdapter} and {@link VungleAdapter}.
 */
abstract class VungleListener {

  void onAdClick(String placementId) {}

  void onAdEnd(String placementId) {}

  void onAdRewarded(String placementId) {}

  void onAdLeftApplication(String placementId) {}

  void onAdClosed(String placementId) {}

  void onAdStart(String placement) {}

  void onAdFail(String placement) {}

  void onAdAvailable() {}

  void onAdFailedToLoad(int errorCode) {}
}
