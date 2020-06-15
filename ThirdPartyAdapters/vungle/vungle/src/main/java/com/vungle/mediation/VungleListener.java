package com.vungle.mediation;

/**
 * A listener class used to send Vungle events from {@link VungleManager} to {@link
 * VungleInterstitialAdapter} and {@link VungleAdapter}.
 */
abstract class VungleListener {

  void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {}

  void onAdStart(String placement) {}

  void onAdFail(String placement) {}

  void onAdAvailable() {}

  void onAdFailedToLoad(int errorCode) {}
}
