package com.google.ads.mediation.adcolony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class AdColonyRewardedEventForwarder extends AdColonyInterstitialListener
    implements AdColonyRewardListener {

  private static AdColonyRewardedEventForwarder instance = null;

  private static HashMap<String, WeakReference<AdColonyRewardedRenderer>> mListeners;

  public static AdColonyRewardedEventForwarder getInstance() {
    if (instance == null) {
      instance = new AdColonyRewardedEventForwarder();
    }
    return instance;
  }

  private AdColonyRewardedEventForwarder() {
    mListeners = new HashMap<>();
  }

  void addListener(@NonNull String zoneID, @NonNull AdColonyRewardedRenderer listener) {
    WeakReference<AdColonyRewardedRenderer> weakListener = new WeakReference<>(listener);
    mListeners.put(zoneID, weakListener);
  }

  private void removeListener(@NonNull String zoneID) {
    mListeners.remove(zoneID);
  }

  @Nullable
  private AdColonyRewardedRenderer getListener(@NonNull String zoneID) {
    WeakReference<AdColonyRewardedRenderer> reference = mListeners.get(zoneID);
    return reference != null ? reference.get() : null;
  }

  boolean isListenerAvailable(@NonNull String zoneID) {
    return getListener(zoneID) != null;
  }

  //region AdColonyInterstitialListener implementation
  @Override
  public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    AdColonyRewardedRenderer listener = getListener(adColonyInterstitial.getZoneID());
    if (listener != null) listener.onRequestFilled(adColonyInterstitial);
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    AdColonyRewardedRenderer listener = getListener(zone.getZoneID());
    if (listener != null) {
      listener.onRequestNotFilled(zone);
      removeListener(zone.getZoneID());
    }
  }

  @Override
  public void onExpiring(AdColonyInterstitial ad) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) listener.onExpiring(ad);
  }

  @Override
  public void onClicked(AdColonyInterstitial ad) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) listener.onClicked(ad);
  }

  @Override
  public void onOpened(AdColonyInterstitial ad) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) listener.onOpened(ad);
  }

  @Override
  public void onLeftApplication(AdColonyInterstitial ad) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) listener.onLeftApplication(ad);
  }

  @Override
  public void onClosed(AdColonyInterstitial ad) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) {
      listener.onClosed(ad);
      removeListener(ad.getZoneID());
    }
  }

  @Override
  public void onIAPEvent(AdColonyInterstitial ad, String productId, int engagementType) {
    AdColonyRewardedRenderer listener = getListener(ad.getZoneID());
    if (listener != null) listener.onIAPEvent(ad, productId, engagementType);
  }
  //endregion

  //region AdColonyRewardListener implementation
  @Override
  public void onReward(AdColonyReward adColonyReward) {
    AdColonyRewardedRenderer listener = getListener(adColonyReward.getZoneID());
    if (listener != null) listener.onReward(adColonyReward);
  }
  //endregion
}
