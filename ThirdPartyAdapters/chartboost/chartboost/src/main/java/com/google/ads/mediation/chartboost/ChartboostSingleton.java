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

package com.google.ads.mediation.chartboost;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Libraries.CBLogging;
import com.chartboost.sdk.Model.CBError;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The {@link ChartboostSingleton} class is used to load Chartboost ads and manage multiple
 * {@link ChartboostAdapter} instances.
 */
public final class ChartboostSingleton {

    /**
     * A synchronized set of weak references of Chartboost adapter delegates used by Chartboost
     * adapters to mediate interstitial callbacks.
     */
    private static Set<WeakReference<AbstractChartboostAdapterDelegate>>
            mInterstitialDelegatesSet = Collections.synchronizedSet(
            new HashSet<WeakReference<AbstractChartboostAdapterDelegate>>());

    /**
     * A synchronized set of weak references of Chartboost adapter delegates used by Chartboost
     * adapters to mediate rewarded video callbacks.
     */
    private static Set<WeakReference<AbstractChartboostAdapterDelegate>>
            mRewardedVideoDelegatesSet = Collections.synchronizedSet(
            new HashSet<WeakReference<AbstractChartboostAdapterDelegate>>());

    /**
     * A weak reference to the {@link AbstractChartboostAdapterDelegate} of the
     * {@link ChartboostAdapter} that is currently displaying an ad.
     */
    private static WeakReference<AbstractChartboostAdapterDelegate> mAdShowingAdapterDelegate;

    /**
     * Flag to keep track of whether or not {@link Chartboost} has initialized.
     */
    private static boolean mIsChartboostInitialized;

    /**
     * The only instance of
     * {@link com.google.ads.mediation.chartboost.ChartboostSingleton.ChartboostSingletonDelegate}.
     */
    private static ChartboostSingletonDelegate chartboostSingletonDelegate;

    /**
     * This method will return an instance of
     * {@link com.google.ads.mediation.chartboost.ChartboostSingleton.ChartboostSingletonDelegate}.
     *
     * @return the only instance of
     * {@link com.google.ads.mediation.chartboost.ChartboostSingleton.ChartboostSingletonDelegate}.
     */
    private static ChartboostSingletonDelegate getInstance() {
        if (chartboostSingletonDelegate == null) {
            chartboostSingletonDelegate = new ChartboostSingletonDelegate();
        }
        return chartboostSingletonDelegate;
    }

    /**
     * This method will clean up the {@link #mInterstitialDelegatesSet} and add the given weak
     * reference of {@link AbstractChartboostAdapterDelegate} to the set.
     *
     * @param delegate that needs to be added to {@link #mInterstitialDelegatesSet}.
     */
    private static void addInterstitialDelegate(AbstractChartboostAdapterDelegate delegate) {
        removeNullAndDuplicateReferences(mInterstitialDelegatesSet, delegate);
        mInterstitialDelegatesSet.add(new WeakReference<>(delegate));
    }

    /**
     * This method will first clean up the {@link #mInterstitialDelegatesSet} and add the given
     * weak reference of {@link AbstractChartboostAdapterDelegate} to the set.
     *
     * @param delegate that needs to be added to {@link #mRewardedVideoDelegatesSet}.
     */
    private static void addRewardedVideoDelegate(AbstractChartboostAdapterDelegate delegate) {
        removeNullAndDuplicateReferences(mRewardedVideoDelegatesSet, delegate);
        mRewardedVideoDelegatesSet.add(new WeakReference<>(delegate));
    }

    /**
     * This method will remove the weak reference from the set if the referenced object has been
     * cleared or if it is a duplicate reference.
     *
     * @param weakReferenceSet that needs to be cleaned up.
     * @param currentDelegate  used to check if a weak reference to this object already exists in
     *                         the given set.
     */
    private static void removeNullAndDuplicateReferences(
            Set<WeakReference<AbstractChartboostAdapterDelegate>> weakReferenceSet,
            AbstractChartboostAdapterDelegate currentDelegate) {
        Iterator<WeakReference<AbstractChartboostAdapterDelegate>> iterator =
                weakReferenceSet.iterator();
        while (iterator.hasNext()) {
            AbstractChartboostAdapterDelegate delegate = iterator.next().get();
            if (delegate == null) {
                iterator.remove();
            } else if (currentDelegate.equals(delegate)) {
                iterator.remove();
            }
        }
    }

    /**
     * This method will initialize Chartboost SDK for interstitial ads and return whether or not
     * it successfully initialized.
     *
     * @param context         Required to initialize Chartboost SDK.
     * @param adapterDelegate The adapter delegate to which to forward Chartboost callbacks.
     * @return {@code true} if Charboost is initialized, {@code false} otherwise.
     */
    public static boolean startChartboostInterstitial(
            Context context, AbstractChartboostAdapterDelegate adapterDelegate) {
        // Add this adapter delegate to mInterstitialDelegatesSet so that the events from
        // Chartboost SDK can be forwarded.
        addInterstitialDelegate(adapterDelegate);

        // Check if the context provided can be used to initialize Chartboost.
        if (!isValidContext(context)) {
            return false;
        }

        startChartboost((Activity) context, adapterDelegate.getChartboostParams());
        return true;
    }

    /**
     * This method will initialize Chartboost SDK for rewarded video ads and return whether or not
     * it successfully initialized.
     *
     * @param context         Required to initialize Chartboost SDK.
     * @param adapterDelegate The adapter delegate to which to forward Chartboost callbacks.
     * @return {@code true} if Charboost is initialized, {@code false} otherwise.
     */
    public static boolean startChartboostRewardedVideo(
            Context context, AbstractChartboostAdapterDelegate adapterDelegate) {
        // Add this adapter delegate to mInterstitialDelegatesSet so that the events from
        // Chartboost SDK can be forwarded.
        addRewardedVideoDelegate(adapterDelegate);

        // Check if the context provided can be used to initialize Chartboost.
        if (!isValidContext(context)) {
            return false;
        }

        ChartboostParams params = adapterDelegate.getChartboostParams();
        startChartboost((Activity) context, params);
        Chartboost.cacheRewardedVideo(params.getLocation());
        return true;
    }

    /**
     * Chartboost requires an Activity context to Initialize. This method will return false if
     * the context provided is either null or is not an Activity context.
     *
     * @param context to be checked if it is valid.
     * @return {@code true} if the context provided is valid, {@code false} otherwise.
     */
    private static boolean isValidContext(Context context) {
        if (context == null) {
            Log.w(ChartboostAdapter.TAG, "Context cannot be null");
            return false;
        }

        if (!(context instanceof Activity)) {
            Log.w(ChartboostAdapter.TAG,
                    "Context is not an Activity. Chartboost requires an Activity context to load "
                            + "ads.");
            return false;
        }
        return true;
    }

    /**
     * This method will initialize the Chartboost SDK if it is not already initialized and set its
     * delegate.
     *
     * @param activity required to initialize {@link Chartboost}.
     * @param params   The Chartboost params containing server parameters and network extras
     *                 to be used to start {@link Chartboost}.
     */
    private static void startChartboost(Activity activity, ChartboostParams params) {

        if (params.getFramework() != null
                && !TextUtils.isEmpty(params.getFrameworkVersion())) {
            Chartboost.setFramework(params.getFramework(), params.getFrameworkVersion());
        }

        if (!mIsChartboostInitialized) {
            Chartboost.startWithAppId(activity, params.getAppId(), params.getAppSignature());

            Chartboost.setMediation(Chartboost.CBMediation.CBMediationAdMob,
                    ChartboostAdapter.ADAPTER_VERSION_NAME);
            Chartboost.setLoggingLevel(CBLogging.Level.INTEGRATION);
            Chartboost.setDelegate(getInstance());
            Chartboost.setAutoCacheAds(true);

            // Chartboost depends on Activity's lifecycle events to initialize its SDK. Chartboost
            // requires onCreate, onStart and onResume callbacks to initialize its SDK.  By the time
            // AdMob SDK requests this adapter to initialize the SDK, all three callbacks might have
            // already been called. So, we call Chartboost's onCreate, onStart and onResume methods
            // so that the Chartboost SDK will be initialized.
            Chartboost.onCreate(activity);
            Chartboost.onStart(activity);
            Chartboost.onResume(activity);
        } else {
            getInstance().didInitialize();
        }
    }

    /**
     * This method will check if {@link Chartboost} already has ads loaded, if so sends the
     * necessary callbacks, else requests {@link Chartboost} to cache interstitial ads.
     *
     * @param delegate Required to obtain the location for which to load the ad and to forward
     *                 the events.
     */
    protected static void loadInterstitialAd(AbstractChartboostAdapterDelegate delegate) {
        // Get the location for which the ads need to be loaded.
        String location = delegate.getChartboostParams().getLocation();
        if (Chartboost.hasInterstitial(location)) {
            // Interstitial ad already cached and is ready to be shown, send onAdLoaded event to
            // the adapter.
            delegate.didCacheInterstitial(location);
        } else {
            // Ad not cached for mLocation yet, request Chartboost to cache interstitial ads for
            // the given location.
            Chartboost.cacheInterstitial(location);
        }
    }

    /**
     * This method will show an interstitial ad.
     *
     * @param delegate that will be showing the ad, needed to set the current showing adapter's
     *                 delegate.
     */
    protected static void showInterstitialAd(AbstractChartboostAdapterDelegate delegate) {
        mAdShowingAdapterDelegate = new WeakReference<>(delegate);
        // Displays a cached interstitial if available, else loads from server.
        Chartboost.showInterstitial(delegate.getChartboostParams().getLocation());
    }

    /**
     * This method will check if {@link Chartboost} already has rewarded video ads loaded, if so,
     * will send the necessary callbacks, else will request {@link Chartboost} to start caching
     * rewarded video ads.
     *
     * @param delegate needed to get the location for which to load Chartboost ads and to forward
     *                 necessary events.
     */
    protected static void loadRewardedVideoAd(AbstractChartboostAdapterDelegate delegate) {
        // Get the location for which the ads need to be loaded.
        String location = delegate.getChartboostParams().getLocation();
        if (Chartboost.hasRewardedVideo(location)) {
            // Video ad already cached and ready to show, send onAdLoaded event to the adapter.
            delegate.didCacheRewardedVideo(location);
        } else {
            // Ad not cached for mLocation yet, request Chartboost to cache rewarded video for
            // the given location.
            Chartboost.cacheRewardedVideo(location);
        }
    }

    /**
     * This method will show rewarded video ad.
     *
     * @param delegate needed to set the currently showing ad and get the location for which to
     *                 show ads.
     */
    protected static void showRewardedVideoAd(AbstractChartboostAdapterDelegate delegate) {
        mAdShowingAdapterDelegate = new WeakReference<>(delegate);
        // Displays a cached video if available, else loads from server.
        Chartboost.showRewardedVideo(delegate.getChartboostParams().getLocation());
    }

    /**
     * The
     * {@link com.google.ads.mediation.chartboost.ChartboostSingleton.ChartboostSingletonDelegate}
     * is used to forward events from Chartboost SDK to Google Mobile Ads SDK for adapters based
     * on which adapters are currently loading ads and wich adapter is currently displaying ad.
     */
    private static final class ChartboostSingletonDelegate extends ChartboostDelegate {

        @Override
        public void didCacheInterstitial(String location) {
            // Interstitial ad has been loaded from the Chartboost API servers and cached locally.
            super.didCacheInterstitial(location);
            Iterator<WeakReference<AbstractChartboostAdapterDelegate>> iterator =
                    mInterstitialDelegatesSet.iterator();
            while (iterator.hasNext()) {
                AbstractChartboostAdapterDelegate delegate = iterator.next().get();
                if (delegate != null
                        && location.equals(delegate.getChartboostParams().getLocation())) {
                    delegate.didCacheInterstitial(location);
                    iterator.remove();
                }
            }
        }

        @Override
        public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error) {
            super.didFailToLoadInterstitial(location, error);
            if (error == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
                // Chartboost SDK failed to show an ad. Notify the current showing adapter delegate.
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didFailToLoadInterstitial(location, error);
                }
                return;
            }

            // Interstitial ad has attempted to load from the Chartboost API servers but failed.
            Iterator<WeakReference<AbstractChartboostAdapterDelegate>> iterator =
                    mInterstitialDelegatesSet.iterator();
            while (iterator.hasNext()) {
                AbstractChartboostAdapterDelegate delegate = iterator.next().get();
                if (delegate != null
                        && location.equals(delegate.getChartboostParams().getLocation())) {
                    delegate.didFailToLoadInterstitial(location, error);
                    iterator.remove();
                }
            }
        }

        @Override
        public void didDisplayInterstitial(String location) {
            // Interstitial ad has been displayed on the screen.
            super.didDisplayInterstitial(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate adapter = mAdShowingAdapterDelegate.get();
                if (adapter != null) {
                    adapter.didDisplayInterstitial(location);
                }
            }
        }

        @Override
        public void didDismissInterstitial(String location) {
            // Interstitial ad has been dismissed.
            super.didDismissInterstitial(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didDismissInterstitial(location);
                }
            }
        }

        @Override
        public void didClickInterstitial(String location) {
            // Interstitial ad has been clicked.
            super.didClickInterstitial(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didClickInterstitial(location);
                }
            }
        }

        @Override
        public void didInitialize() {
            // Chartboost SDK has been successfully initialized and video pre-fetching has been
            // completed.
            super.didInitialize();
            mIsChartboostInitialized = true;
            for (WeakReference<AbstractChartboostAdapterDelegate> weakReference
                    : mRewardedVideoDelegatesSet) {
                AbstractChartboostAdapterDelegate delegate = weakReference.get();
                if (delegate != null) {
                    delegate.didInitialize();
                }
            }
        }

        @Override
        public void didCacheRewardedVideo(String location) {
            // Rewarded video has been loaded from the Chartboost API servers and cached locally.
            super.didCacheRewardedVideo(location);
            for (WeakReference<AbstractChartboostAdapterDelegate> weakReference
                    : mRewardedVideoDelegatesSet) {
                AbstractChartboostAdapterDelegate delegate = weakReference.get();
                if (delegate != null
                        && location.equals(delegate.getChartboostParams().getLocation())) {
                    delegate.didCacheRewardedVideo(location);
                }
            }
        }

        @Override
        public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
            super.didFailToLoadRewardedVideo(location, error);
            if (error == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
                // Chartboost SDK failed to show an ad. Notify the current showing adapter delegate.
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didFailToLoadRewardedVideo(location, error);
                }
                return;
            }

            // Rewarded video has attempted to load from the Chartboost API servers but failed.
            for (WeakReference<AbstractChartboostAdapterDelegate> weakReference
                    : mRewardedVideoDelegatesSet) {
                AbstractChartboostAdapterDelegate delegate = weakReference.get();
                if (delegate != null
                        && location.equals(delegate.getChartboostParams().getLocation())) {
                    delegate.didFailToLoadRewardedVideo(location, error);
                }
            }
        }

        @Override
        public void didClickRewardedVideo(String location) {
            // Rewarded video has been clicked.
            super.didClickRewardedVideo(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didClickRewardedVideo(location);
                }
            }
        }

        @Override
        public void didDisplayRewardedVideo(String location) {
            // Rewarded video has been displayed on the screen.
            super.didDisplayRewardedVideo(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didDisplayRewardedVideo(location);
                }
            }
        }

        @Override
        public void didCompleteRewardedVideo(String location, int reward) {
            // Rewarded video has been viewed completely and user is eligible for reward.
            super.didCompleteRewardedVideo(location, reward);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didCompleteRewardedVideo(location, reward);
                }
            }
        }

        @Override
        public void didDismissRewardedVideo(String location) {
            // Rewarded video has been dismissed.
            super.didDismissRewardedVideo(location);
            if (mAdShowingAdapterDelegate != null) {
                AbstractChartboostAdapterDelegate delegate = mAdShowingAdapterDelegate.get();
                if (delegate != null) {
                    delegate.didDismissRewardedVideo(location);
                }
            }
        }
    }
}
