package com.google.ads.mediation.adcolony;

import androidx.annotation.NonNull;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonyZone;

import java.lang.ref.WeakReference;
import java.util.HashMap;

class AdColonyRewardedEventForwarder extends AdColonyInterstitialListener
        implements AdColonyRewardListener {

    private static AdColonyRewardedEventForwarder instance = null;

    private static HashMap<String, WeakReference<AdColonyRewardedRenderer>> mListeners;

    static AdColonyRewardedEventForwarder getInstance() {
        if (instance == null) {
            instance = new AdColonyRewardedEventForwarder();
        }
        return instance;
    }

    private AdColonyRewardedEventForwarder() {
        mListeners = new HashMap<>();
        AdColony.setRewardListener(AdColonyRewardedEventForwarder.this);
    }

    void addListener(@NonNull String zoneID, @NonNull AdColonyRewardedRenderer listener) {
        WeakReference<AdColonyRewardedRenderer> weakListener = new WeakReference<>(listener);
        mListeners.put(zoneID, weakListener);
    }

    boolean isListenerAvailable(@NonNull String zoneID) {
        return (mListeners.containsKey(zoneID) && mListeners.get(zoneID).get() != null);
    }

    //region AdColonyInterstitialListener implementation
    @Override
    public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
        String zoneID = adColonyInterstitial.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onRequestFilled(adColonyInterstitial);
        }
    }

    @Override
    public void onRequestNotFilled(AdColonyZone zone) {
        String zoneID = zone.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onRequestNotFilled(zone);
            mListeners.remove(zoneID);
        }
    }

    @Override
    public void onExpiring(AdColonyInterstitial ad) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onExpiring(ad);
        }
    }

    @Override
    public void onClicked(AdColonyInterstitial ad) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onClicked(ad);
        }
    }

    @Override
    public void onOpened(AdColonyInterstitial ad) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onOpened(ad);
        }
    }

    @Override
    public void onLeftApplication(AdColonyInterstitial ad) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onLeftApplication(ad);
        }
    }

    @Override
    public void onClosed(AdColonyInterstitial ad) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onClosed(ad);
            mListeners.remove(zoneID);
        }
    }

    @Override
    public void onIAPEvent(AdColonyInterstitial ad, String product_id, int engagement_type) {
        String zoneID = ad.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onIAPEvent(ad, product_id, engagement_type);
        }
    }
    //endregion

    //region AdColonyRewardListener implementation
    @Override
    public void onReward(AdColonyReward adColonyReward) {
        String zoneID = adColonyReward.getZoneID();
        if (isListenerAvailable(zoneID)) {
            mListeners.get(zoneID).get().onReward(adColonyReward);
        }
    }
    //endregion
}
