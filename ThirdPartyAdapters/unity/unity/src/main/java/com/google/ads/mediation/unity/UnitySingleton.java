// Copyright 2016 Google Inc.
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

package com.google.ads.mediation.unity;

import android.app.Activity;
import android.util.Log;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple
 * {@link UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton {
    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener}.
     */
    private UnitySingletonListener unitySingletonListenerInstance;

    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton}.
     */
    private static UnitySingleton unitySingletonInstance;

    private ArrayList<WeakReference<Listener>> mListenersWeakReference;

    HashSet<String> mPlacementsInUse;

    /**
     * This method will return a
     * {@link com.google.ads.mediation.unity.UnitySingleton} instance.
     *
     * @return the {@link #unitySingletonInstance}.
     */
    public static UnitySingleton getInstance() {
        if (unitySingletonInstance == null) {
            unitySingletonInstance = new UnitySingleton();
        }
        return unitySingletonInstance;
    }

    private UnitySingleton() {
        mListenersWeakReference = new ArrayList<>();
        mPlacementsInUse = new HashSet<>();
    }

    /**
     * This method will return the
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} instance.
     *
     * @return the {@link #unitySingletonListenerInstance}.
     */
    private UnitySingletonListener getUnitySingletonListenerInstance() {
        if (unitySingletonListenerInstance == null) {
            unitySingletonListenerInstance = new UnitySingletonListener();
        }
        return unitySingletonListenerInstance;
    }

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param activity The Activity context.
     * @param gameId   Unity Ads Game ID.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    public boolean initializeUnityAds(Activity activity, String gameId, Listener listener) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            listener.onInitializeError("The current device is not supported by Unity Ads.");
            return false;
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            listener.onInitializeSuccess();
            return true;
        }

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", "3.3.0");
        mediationMetaData.commit();

        getInstance().mListenersWeakReference.add(new WeakReference<Listener>(listener));

        UnitySingletonListener unitySingletonListener = unitySingletonInstance.getUnitySingletonListenerInstance();
        UnityAds.addListener(unitySingletonListener);
        UnityAds.initialize(activity, gameId,false, true);

        return true;
    }

    /**
     * The {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} is used
     * to handle onUnityAdsError and onUnityFinish without placementId.
     */
    private final class UnitySingletonListener
            implements IUnityAdsExtendedListener {

        /**
         * {@link IUnityAdsExtendedListener} implementation
         */
        @Override
        public void onUnityAdsReady(String placementId) {
        }

        @Override
        public void onUnityAdsStart(String placementId) {
        }

        @Override
        public void onUnityAdsClick(String placementId) {
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
           if (newState == UnityAds.PlacementState.WAITING || newState == UnityAds.PlacementState.READY) {
               for (WeakReference<Listener> listenerWeakReference : getInstance().mListenersWeakReference) {
                   if (listenerWeakReference.get() != null) {
                       listenerWeakReference.get().onInitializeSuccess();
                   }
               }
               mListenersWeakReference.clear();
               UnityAds.removeListener(getInstance().getUnitySingletonListenerInstance());
           }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
            // An error occurred with Unity Ads.
            if (unityAdsError == UnityAds.UnityAdsError.NOT_INITIALIZED || unityAdsError == UnityAds.UnityAdsError.INITIALIZE_FAILED
                    || unityAdsError == UnityAds.UnityAdsError.INIT_SANITY_CHECK_FAIL || unityAdsError == UnityAds.UnityAdsError.INVALID_ARGUMENT) {
                for (WeakReference<Listener> listenerWeakReference : getInstance().mListenersWeakReference) {
                    if (listenerWeakReference.get() != null) {
                        listenerWeakReference.get().onInitializeError(message);
                    }
                }
                mListenersWeakReference.clear();
                UnityAds.removeListener(getInstance().getUnitySingletonListenerInstance());
            }
        }
    }

    interface Listener {
        void onInitializeSuccess();
        void onInitializeError(String message);
    }
}
