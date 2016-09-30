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

import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple
 * {@link UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton {

    /**
     * A synchronized hash set used to hold UnityAdapterDelegates.
     */
    private static Set<WeakReference<UnityAdapterDelegate>> mUnityAdapterDelegatesSet =
            Collections.synchronizedSet(new HashSet<WeakReference<UnityAdapterDelegate>>());

    /**
     * A weak reference to the {@link UnityAdapterDelegate} of the {@link UnityAdapter} that is
     * currently displaying an ad.
     */
    private static WeakReference<UnityAdapterDelegate> mAdShowingAdapterDelegate;

    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener}.
     */
    private static UnitySingletonListener unitySingletonListenerInstance;

    /**
     * This method will return the
     * {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} instance.
     *
     * @return the {@link #unitySingletonListenerInstance}.
     */
    private static UnitySingletonListener getInstance() {
        if (unitySingletonListenerInstance == null) {
            unitySingletonListenerInstance = new UnitySingletonListener();
        }
        return unitySingletonListenerInstance;
    }

    /**
     * Removes the weak references from {@link #mUnityAdapterDelegatesSet} whose object is
     * cleared, and add a weak reference of the given {@link UnityAdapterDelegate} to the set.
     *
     * @param unityAdapterDelegate to be added to {@link #mUnityAdapterDelegatesSet}.
     */
    private static void addUnityAdapterDelegate(UnityAdapterDelegate unityAdapterDelegate) {
        Iterator<WeakReference<UnityAdapterDelegate>> iterator =
                mUnityAdapterDelegatesSet.iterator();
        while (iterator.hasNext()) {
            UnityAdapterDelegate delegate = iterator.next().get();
            if (delegate == null) {
                iterator.remove();
            } else if (delegate.equals(unityAdapterDelegate)) {
                return;
            }
        }
        mUnityAdapterDelegatesSet.add(new WeakReference<>(unityAdapterDelegate));
    }

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param delegate Used to forward events to the adapter.
     * @param activity Used to initialize {@link UnityAds}.
     * @param gameId   Unity Ads Game ID.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    public static boolean initializeUnityAds(UnityAdapterDelegate delegate,
                                             Activity activity,
                                             String gameId) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            return false;
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            return true;
        }

        // Add the delegate to the set so that the callbacks from Unity Ads can be forwarded to
        // the adapter.
        addUnityAdapterDelegate(delegate);

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion("Unreleased");
        mediationMetaData.commit();

        UnityAds.initialize(activity, gameId, UnitySingleton.getInstance());

        return true;
    }

    /**
     * This method will load Unity ads for a given Zone ID and send the ad loaded event if the
     * ads have already loaded for that zone.
     *
     * @param zoneId   Unity Ads Zone ID for which to load ad.
     * @param delegate Used to forward Unity Ads events to the adapter.
     */
    protected static void loadAd(String zoneId, UnityAdapterDelegate delegate) {
        // Unity ads does not have a load method and ads begin to load when initialize is called.
        // So, we check if unity ads is initialized to determine whether or not the ads are loading.
        // If Unity Ads is initialized, we call the appropriate callbacks by checking the isReady
        // method. If ads are currently being loaded, wait for the callbacks from
        // unitySingletonListenerInstance.
        if (UnityAds.isInitialized()) {
            if (UnityAds.isReady(zoneId)) {
                delegate.onUnityAdsReady(zoneId);
            } else {
                delegate.onUnityAdsError(UnityAds.UnityAdsError.INTERNAL_ERROR, zoneId);
            }
        }
    }

    /**
     * This method will show an Unity Ad.
     *
     * @param zoneId   Unity Ads Zone ID for which to show ad.
     * @param delegate Used to forward Unity Ads events to the adapter.
     * @param activity An Android {@link Activity} required to show an ad.
     */
    protected static void showAd(String zoneId, UnityAdapterDelegate delegate, Activity activity) {
        mAdShowingAdapterDelegate = new WeakReference<>(delegate);

        // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
        // Unity Ads fails to shown an ad).
        UnityAds.show(activity, zoneId);
    }

    /**
     * The {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} is used
     * to forward events from Unity Ads SDK to {@link UnityAdapter} based on the delegates added
     * to {@link #mUnityAdapterDelegatesSet} and which adapter is currently showing an ad.
     */
    private static final class UnitySingletonListener implements IUnityAdsListener {

        @Override
        public void onUnityAdsReady(String zoneId) {
            // Unity Ads is ready to show ads for the given zoneId, send ready callback to the
            // appropriate delegates.
            Iterator<WeakReference<UnityAdapterDelegate>> iterator =
                    mUnityAdapterDelegatesSet.iterator();
            while (iterator.hasNext()) {
                UnityAdapterDelegate delegate = iterator.next().get();
                if (delegate != null && delegate.getZoneId().equals(zoneId)) {
                    delegate.onUnityAdsReady(zoneId);
                    iterator.remove();
                }
            }
        }

        @Override
        public void onUnityAdsStart(String zoneId) {
            // Unity Ads video ad started, send video started event to currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsStart(zoneId);
                }
            }
        }

        @Override
        public void onUnityAdsFinish(String zoneId, UnityAds.FinishState finishState) {
            // An Unity Ads ad has been closed, forward the finish event to the currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsFinish(zoneId, finishState);
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String zoneId) {
            // An error occured with Unity Ads, send error event to the appropriate delegates.
            Iterator<WeakReference<UnityAdapterDelegate>> iterator =
                    mUnityAdapterDelegatesSet.iterator();
            while (iterator.hasNext()) {
                UnityAdapterDelegate delegate = iterator.next().get();
                if (delegate != null && delegate.getZoneId().equals(zoneId)) {
                    delegate.onUnityAdsError(unityAdsError, zoneId);
                    iterator.remove();
                }
            }
        }
    }
}
