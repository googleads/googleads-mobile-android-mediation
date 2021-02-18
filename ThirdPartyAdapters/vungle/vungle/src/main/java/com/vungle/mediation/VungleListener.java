package com.vungle.mediation;

/**
 * A listener class used to send Vungle events from {@link VungleManager} to {@link
 * VungleInterstitialAdapter} and {@link VungleAdapter}.
 */
public abstract class VungleListener {

  public void onAdClick(String placementId) {
  }

  public void onAdEnd(String placementId) {
  }

  public void onAdRewarded(String placementId) {
  }

  public void onAdLeftApplication(String placementId) {
  }

  public void onAdStart(String placement) {
  }

  void onAdFail(String placement) {
  }

  void onAdAvailable() {
  }

  public void onAdFailedToLoad(int errorCode) {
  }
}
