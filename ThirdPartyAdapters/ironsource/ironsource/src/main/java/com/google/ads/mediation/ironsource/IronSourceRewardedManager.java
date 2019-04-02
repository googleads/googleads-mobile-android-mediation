package com.google.ads.mediation.ironsource;

import android.support.annotation.NonNull;

import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A centralized {@link ISDemandOnlyRewardedVideoListener} to forward IronSource ad events
 * to all {@link IronSourceMediationAdapter} instances.
 */
class IronSourceRewardedManager {

    private static IronSourceRewardedManager instance = null;

    private HashMap<String, WeakReference<IronSourceRewardedAvailabilityListener>> availabilityListeners;
    private HashMap<String, WeakReference<IronSourceMediationAdapter>> availableAds;

    static IronSourceRewardedManager getInstance() {
        if (instance == null){
            instance = new IronSourceRewardedManager();
        }
        return instance;
    }

    private IronSourceRewardedManager() {
        this.availabilityListeners = new HashMap<>();
        this.availableAds = new HashMap<>();
    }

    void addListener(WeakReference<IronSourceRewardedAvailabilityListener> listener,
                     String instanceID) {
        if (listener.get() != null) {
            this.availabilityListeners.put(instanceID, listener);
        }
    }

    void onRewardedVideoAvailabilityChanged(String instanceID, boolean isAvailable) {
        Iterator<String> iterator = this.availabilityListeners.keySet().iterator();

        while (iterator.hasNext()) {
            String instanceFromListeners = iterator.next();

            if (instanceID.equals(instanceFromListeners)) {
                if (this.availabilityListeners.get(instanceFromListeners).get() != null) {
                    if (isAvailable) {
                        this.availabilityListeners.get(instanceFromListeners).get().onRewardedAdAvailable();
                    } else {
                        this.availabilityListeners.get(instanceFromListeners).get().onRewardedAdNotAvailable();
                    }
                }

                iterator.remove();
            }
        }
    }

    boolean isISRewardedAdLoading(@NonNull String instanceID) {
        return (this.availabilityListeners.containsKey(instanceID) &&
                this.availabilityListeners.get(instanceID).get() != null);
    }

    void registerISRewardedAd(@NonNull String instanceID,
                              @NonNull WeakReference<IronSourceMediationAdapter> weakAdapter) {
        if (weakAdapter.get() != null) {
            this.availableAds.put(instanceID, weakAdapter);
        }
    }

    void unregisterISRewardedAd(@NonNull String instanceID) {
        this.availableAds.remove(instanceID);
    }

    boolean isISRewardedAdRegistered(@NonNull String instanceID) {
        return (this.availableAds.containsKey(instanceID) &&
                this.availableAds.get(instanceID).get() != null);
    }

}
