package com.google.ads.mediation.adcolony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyReward;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class AdColonyRewardListener implements com.adcolony.sdk.AdColonyRewardListener {

  private static AdColonyRewardListener instance;
  private final List<WeakReference<AdColonyRewardListenerExtended>> mListeners = new ArrayList<>();

  static AdColonyRewardListener getInstance() {
    if (instance == null) {
      synchronized (AdColonyRewardListener.class) {
        if (instance == null) {
          instance = new AdColonyRewardListener();
        }
      }
    }
    return instance;
  }

  AdColonyRewardListener() {
    AdColony.setRewardListener(this);
  }

  void addListener(@NonNull AdColonyRewardListenerExtended listener) {
    WeakReference<AdColonyRewardListenerExtended> weakListener = new WeakReference<>(listener);
    mListeners.add(weakListener);
  }

  private void removeListener(@NonNull AdColonyRewardListenerExtended listener) {
    for (Iterator<WeakReference<AdColonyRewardListenerExtended>> iterator = mListeners.iterator(); iterator.hasNext(); ) {
      WeakReference<AdColonyRewardListenerExtended> weakReference = iterator.next();
      AdColonyRewardListenerExtended reference = weakReference.get();
      if (reference == null) {
        iterator.remove();
      } else if (reference == listener) {
        mListeners.remove(weakReference);
        break;
      }
    }
  }

  @Nullable
  private AdColonyRewardListenerExtended getListener(@NonNull String zoneId) {
    if (!mListeners.isEmpty()) {
      for (Iterator<WeakReference<AdColonyRewardListenerExtended>> iterator = mListeners.iterator(); iterator.hasNext(); ) {
        AdColonyRewardListenerExtended reference = iterator.next().get();
        if (reference == null) {
          iterator.remove();
        } else if (zoneId.equals(reference.getZoneId())) {
          return reference;
        }
      }
    }
    return null;
  }

  @Override
  public void onReward(AdColonyReward adColonyReward) {
    AdColonyRewardListenerExtended listener = getListener(adColonyReward.getZoneID());
    if (listener != null) {
      listener.onReward(adColonyReward);
      removeListener(listener);
    }
  }

  interface AdColonyRewardListenerExtended extends com.adcolony.sdk.AdColonyRewardListener {
    String getZoneId();
  }
}
