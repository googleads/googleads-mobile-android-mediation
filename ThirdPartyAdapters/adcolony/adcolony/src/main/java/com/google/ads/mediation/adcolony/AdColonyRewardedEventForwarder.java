// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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

  private static HashMap<String, WeakReference<AdColonyRewardedRenderer>> listeners;

  public static AdColonyRewardedEventForwarder getInstance() {
    if (instance == null) {
      instance = new AdColonyRewardedEventForwarder();
    }
    return instance;
  }

  private AdColonyRewardedEventForwarder() {
    listeners = new HashMap<>();
  }

  void addListener(@NonNull String zoneID, @NonNull AdColonyRewardedRenderer listener) {
    WeakReference<AdColonyRewardedRenderer> weakListener = new WeakReference<>(listener);
    listeners.put(zoneID, weakListener);
  }

  private void removeListener(@NonNull String zoneID) {
    listeners.remove(zoneID);
  }

  @Nullable
  private AdColonyRewardedRenderer getListener(@NonNull String zoneID) {
    WeakReference<AdColonyRewardedRenderer> reference = listeners.get(zoneID);
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
