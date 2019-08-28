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
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple
 * {@link UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton {

    /**
     * A list of adapter listeners with their respective placement IDs to prevent duplicate requests.
     */
    private static HashMap<String, WeakReference<UnityAdapterDelegate>> mPlacementsInUse =
            new HashMap<>();

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

    private static WeakReference<Activity> activity;

    /**
     * Used by Unity Ads to track failures in the mediation lifecycle
     */
    private static int impressionOrdinal;
    private static int missedImpressionOrdinal;

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
     * This method will initialize {@link UnityAds}.
     *
     * @param activity    The Activity context.
     * @param gameId      Unity Ads Game ID.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    static boolean initializeUnityAds(Activity activity, String gameId) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            return false;
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            return true;
        }

        UnitySingleton.activity = new WeakReference<>(activity);

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", "3.2.0");
        mediationMetaData.commit();

        UnitySingletonListener unitySingleton = UnitySingleton.getInstance();
        UnityBanners.setBannerListener(unitySingleton);
        UnityAds.initialize(activity, gameId, UnitySingleton.getInstance(), false, true);

        return true;
    }

    /**
     * Initializes {@link UnityAds}.
     *
     * @param delegate    Delegate to the adapter being initialized.
     * @param activity    The Activity context.
     * @param gameId      Unity Ads Game ID.
     * @param placementId Placement ID to load once Unity Ads initializes.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    static boolean initializeUnityAds(UnityAdapterDelegate delegate,
                                             Activity activity,
                                             String gameId,
                                             @NonNull String placementId) {
        if (!TextUtils.isEmpty(placementId) && !mPlacementsInUse.containsKey(placementId)) {
            mPlacementsInUse.put(placementId, new WeakReference<>(delegate));
        }

        return initializeUnityAds(activity, gameId);
    }

    /**
     * Initializes {@link UnityAds}.
     *
     * @param delegate       Delegate to the adapter being initialized.
     * @param activity       The Activity context.
     * @param gameId         Unity Ads Game ID.
     * @param placementId    Placement ID to load once Unity Ads initializes.
     * @param bannerDelegate Delegate to the Banner Adapter being initialized.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    public static boolean initializeUnityAds(UnityAdapterDelegate delegate,
                                             Activity activity,
                                             String gameId,
                                             @NonNull String placementId,
                                             UnityAdapterBannerDelegate bannerDelegate) {
        mBannerDelegate = new WeakReference<>(bannerDelegate);
        return initializeUnityAds(delegate, activity, gameId, placementId);
    }

    /**
     * This method will load Unity ads for a given Placement ID and send the ad loaded event if the
     * ads have already loaded.
     *
     * @param delegate Used to forward Unity Ads events to the adapter.
     */
    protected static void loadAd(UnityAdapterDelegate delegate) {

        // Calling load before UnityAds.inititalize() will cause the placement to load on init
        UnityAds.load(delegate.getPlacementId());

        if (UnityAds.isInitialized()) {
            //If ads are currently being loaded, wait for the callbacks from
            // unitySingletonListenerInstance.
            // Check if an AdMob Ad request has already loaded or is in progress of requesting
            // an Ad from Unity Ads for a single placement, and fail if there's any.
            if (mPlacementsInUse.containsKey(delegate.getPlacementId()) &&
                    mPlacementsInUse.get(delegate.getPlacementId()).get() != null) {
                Log.e(UnityMediationAdapter.TAG,
                        "An ad is already loading for placement ID: " + delegate.getPlacementId());
                delegate.onUnityAdsError(UnityAds.UnityAdsError.INTERNAL_ERROR,
                        delegate.getPlacementId());
                return;
            }

            mPlacementsInUse.put(delegate.getPlacementId(), new WeakReference<>(delegate));
            if (UnityAds.isReady(delegate.getPlacementId())) {
                delegate.onUnityAdsReady(delegate.getPlacementId());
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
        if (UnitySingleton.activity != null) {
            Activity activity = UnitySingleton.activity.get();

            if (activity != null && UnityAds.isInitialized()) {
                mBannerDelegate = new WeakReference<>(delegate);

                if (UnityAds.isReady(delegate.getPlacementId())) {
                    UnityBanners.loadBanner(activity, delegate.getPlacementId());
                } else {
                    UnityBanners.destroy();
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

        if(UnityAds.isReady(delegate.getPlacementId())) {
            // Notify UnityAds that the adapter made a successful show request
            MediationMetaData metadata = new MediationMetaData(activity);
            metadata.setOrdinal(++impressionOrdinal);
            metadata.commit();

            UnityAds.show(activity, delegate.getPlacementId());
        } else {
            // Notify UnityAds that the adapter failed to show
            MediationMetaData metadata = new MediationMetaData(activity);
            metadata.setMissedImpressionOrdinal(++missedImpressionOrdinal);
            metadata.commit();
        }
    }

    /**
     * The {@link com.google.ads.mediation.unity.UnitySingleton.UnitySingletonListener} is used
     * to forward events from Unity Ads SDK to {@link UnityAdapter} based on the delegates added
     * to {@link #mPlacementsInUse} and which adapter is currently showing an ad.
     */
    private static final class UnitySingletonListener
            implements IUnityAdsExtendedListener, IUnityBannerListener {

        /**
         * {@link IUnityAdsExtendedListener} implementation
         */
        @Override
        public void onUnityAdsReady(String placementId) {
            // Unity Ads is ready to show ads for the given placementId. Send ready callback to the
            // appropriate delegate.
            if (mPlacementsInUse.containsKey(placementId) &&
                    mPlacementsInUse.get(placementId).get() != null) {
                mPlacementsInUse.get(placementId).get().onUnityAdsReady(placementId);
            }

            // If 'mBannerDelegate' has a value, then that means a UnityAds banner request is
            // waiting to be sent by the adapter.
            if (mBannerDelegate != null &&
                    mBannerDelegate.get() != null &&
                    UnitySingleton.activity != null &&
                    UnitySingleton.activity.get() != null &&
                    placementId.equals(mBannerDelegate.get().getPlacementId())) {
                UnityBanners.loadBanner(UnitySingleton.activity.get(), placementId);
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
                    mPlacementsInUse.remove(placementId);
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String placementId) {
            // An error occurred with Unity Ads. Send error event to the appropriate delegate.
            if (mPlacementsInUse.containsKey(placementId) &&
                    mPlacementsInUse.get(placementId).get() != null) {
                mPlacementsInUse.get(placementId).get()
                        .onUnityAdsError(unityAdsError, placementId);
                mPlacementsInUse.remove(placementId);
            }
        }

        /**
         * {@link IUnityBannerListener} implementation
         */
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
