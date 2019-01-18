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
import android.view.View;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;

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
    private static WeakReference<UnityAdapterBannerDelegate> mBannerDelegate;

    private static WeakReference<Activity> mActivity;

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

        mActivity = new WeakReference<>(activity);

        // Add the delegate to the set so that the callbacks from Unity Ads can be forwarded to
        // the adapter.
        addUnityAdapterDelegate(delegate);

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion("3.0.0.0");
        mediationMetaData.commit();

        UnitySingletonListener unitySingleton = UnitySingleton.getInstance();
        UnityBanners.setBannerListener(unitySingleton);
        UnityAds.initialize(activity, gameId, UnitySingleton.getInstance());

        return true;
    }

    /**
     * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     */
    protected static void loadAd(UnityAdapterDelegate delegate) {
        // Unity ads does not have a load method and ads begin to load when initialize is called.
        // So, we check if unity ads is initialized to determine whether or not the ads are loading.
        // If Unity Ads is initialized, we call the appropriate callbacks by checking the isReady
        // method. If ads are currently being loaded, wait for the callbacks from
        // unitySingletonListenerInstance.
        if (UnityAds.isInitialized()) {
            if (UnityAds.isReady(delegate.getPlacementId())) {
                delegate.onUnityAdsReady(delegate.getPlacementId());
            } else {
                delegate.onUnityAdsError(
                        UnityAds.UnityAdsError.INTERNAL_ERROR, delegate.getPlacementId());
            }
        }
    }

    /**
     * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     */
    protected static void loadBannerAd(UnityAdapterBannerDelegate delegate) {
        if (mActivity != null) {
            Activity activity = mActivity.get();
            if (activity != null) {
                // UnityAdsBanner is marked ready when "ready to load". Calling
                if (UnityAds.isInitialized()) {
                    if (UnityAds.isReady(delegate.getPlacementId())) {
                        mBannerDelegate = new WeakReference<>(delegate);
                        UnityBanners.loadBanner(activity, delegate.getPlacementId());
                    } else {
                        delegate.onUnityBannerError("Placement " + delegate.getPlacementId() + " is not ready.");
                    }
                }
            }
        }
    }

    /**
     * This method will show an Unity Ad.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     * @param activity An Android {@link Activity} required to show an ad.
     */
    protected static void showAd(UnityAdapterDelegate delegate, Activity activity) {
        mAdShowingAdapterDelegate = new WeakReference<>(delegate);

        // Every call to UnityAds#show will result in an onUnityAdsFinish callback (even when
        // Unity Ads fails to shown an ad).
        UnityAds.show(activity, delegate.getPlacementId());
    }

    /**
     * The {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} is used
     * to forward events from Unity Ads SDK to {@link UnityAdapter} based on the delegates added
     * to {@link #mUnityAdapterDelegatesSet} and which adapter is currently showing an ad.
     */
    private static final class UnitySingletonListener implements IUnityAdsExtendedListener, IUnityBannerListener {

        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send ready callback to the
            // appropriate delegates.
            Iterator<WeakReference<UnityAdapterDelegate>> iterator =
                    mUnityAdapterDelegatesSet.iterator();
            while (iterator.hasNext()) {
                UnityAdapterDelegate delegate = iterator.next().get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityAdsReady(placementId);
                    iterator.remove();
                }
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            // Unity Ads video ad started. Send video started event to currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsStart(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsClick(String placementId) {
            // An Unity Ads ad has been clicked. Send ad clicked event to currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsClick(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsPlacementStateChanged(String placementId,
                                                    UnityAds.PlacementState oldState,
                                                    UnityAds.PlacementState newState) {
            // The onUnityAdsReady and onUnityAdsError callback methods are used to forward Unity
            // Ads SDK states to the adapters. No need to forward this callback to the adapters.
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            // An Unity Ads ad has been closed. Forward the finish event to the currently showing
            // adapter's delegate.
            if (mAdShowingAdapterDelegate != null) {
                UnityAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.onUnityAdsFinish(placementId, finishState);
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // An error occurred with Unity Ads. Send error event to the appropriate delegates.
            Iterator<WeakReference<UnityAdapterDelegate>> iterator =
                    mUnityAdapterDelegatesSet.iterator();
            while (iterator.hasNext()) {
                UnityAdapterDelegate delegate = iterator.next().get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityAdsError(unityAdsError, placementId);
                    iterator.remove();
                }
            }
        }

        @Override
        public void onUnityBannerLoaded(String placementId, View view) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityBannerLoaded(placementId, view);
                }
            }
        }

        @Override
        public void onUnityBannerUnloaded(String placementId) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityBannerUnloaded(placementId);
                }
            }
        }

        @Override
        public void onUnityBannerShow(String placementId) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityBannerShow(placementId);
                }
            }
        }

        @Override
        public void onUnityBannerClick(String placementId) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityBannerClick(placementId);
                }
            }
        }

        @Override
        public void onUnityBannerHide(String placementId) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null && delegate.getPlacementId().equals(placementId)) {
                    delegate.onUnityBannerHide(placementId);
                }
            }
        }

        @Override
        public void onUnityBannerError(String message) {
            if (mBannerDelegate != null) {
                UnityAdapterBannerDelegate delegate = mBannerDelegate.get();
                if (delegate != null) {
                    delegate.onUnityBannerError(message);
                }
            }
        }
    }
}