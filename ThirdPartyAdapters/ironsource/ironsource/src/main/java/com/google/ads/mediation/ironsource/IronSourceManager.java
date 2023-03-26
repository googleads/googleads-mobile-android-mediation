package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.ADAPTER_VERSION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.MEDIATION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_AD_SHOW_UNAUTHORIZED;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS;
import static com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerListener;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyRewardedVideoListener;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A centralized {@link ISDemandOnlyRewardedVideoListener} to forward IronSource ad events to all
 * {@link IronSourceMediationAdapter} instances.
 */
public class IronSourceManager implements ISDemandOnlyRewardedVideoListener, ISDemandOnlyInterstitialListener, ISDemandOnlyBannerListener {
    private static final IronSourceManager instance = new IronSourceManager();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final ConcurrentHashMap<String, WeakReference<IronSourceMediationAdapter>> availableInstances;
    private final ConcurrentHashMap<String, WeakReference<IronSourceAdapter>> availableInterstitialInstances;
    private final ConcurrentHashMap<String, WeakReference<IronSourceBannerAd>> availableBannerInstances;

    private WeakReference<IronSourceMediationAdapter> currentlyShowingRewardedAdapter;
    private IronSourceBannerAd ironSourceBannerAd;

    static IronSourceManager getInstance() {
        return instance;
    }

    private IronSourceManager() {
        availableInstances = new ConcurrentHashMap<>();
        availableInterstitialInstances = new ConcurrentHashMap<>();
        availableBannerInstances = new ConcurrentHashMap<>();
        IronSource.setISDemandOnlyRewardedVideoListener(this);
        IronSource.setISDemandOnlyInterstitialListener(this);
    }

    void initIronSourceSDK(@NonNull Context context, @Nullable String appKey,
                           @NonNull InitializationCallback listener) {
        if (isInitialized.get()) {
            listener.onInitializeSuccess();
            return;
        }

        if (TextUtils.isEmpty(appKey)) {
            AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid app key.", ERROR_DOMAIN);
            listener.onInitializeError(initializationError);
            return;
        }

        IronSource.setMediationType(MEDIATION_NAME + ADAPTER_VERSION_NAME);
        Log.d(TAG, "Initializing IronSource SDK with app key: " + appKey);
        IronSource.initISDemandOnly(context, appKey, IronSource.AD_UNIT.INTERSTITIAL,
                IronSource.AD_UNIT.REWARDED_VIDEO, IronSource.AD_UNIT.BANNER);

        isInitialized.set(true);
        listener.onInitializeSuccess();
    }

    void loadInterstitial(@Nullable Context context, @NonNull String instanceId, @NonNull IronSourceAdapter adapter) {
        if (!(context instanceof Activity)) {
            String errorMessage = String
                    .format(ERROR_REQUIRES_ACTIVITY_CONTEXT + "IronSource requires an Activity context to load ads.");
            AdError contextError = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, errorMessage, ERROR_DOMAIN);
            adapter.onAdFailedToLoad(contextError);
            return;
        }

        Activity activity = (Activity) context;

        if (TextUtils.isEmpty(instanceId)) {
            AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid instance ID.", ERROR_DOMAIN);
            adapter.onAdFailedToLoad(loadError);
            return;
        }

        if (!canLoadInterstitialInstance(instanceId)) {
            String errorMessage = String
                    .format("An ad is already loading for instance ID: %s", instanceId);
            AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
            adapter.onAdFailedToLoad(concurrentError);
            return;
        }

        registerISInterstitialAdapter(instanceId, new WeakReference<>(adapter));
        IronSource.loadISDemandOnlyInterstitial(activity, instanceId);
    }

    void loadRewardedVideo(@NonNull Context context, @NonNull String instanceId, @NonNull IronSourceMediationAdapter adapter) {
        if (!(context instanceof Activity)) {
            String errorMessage = String
                    .format(ERROR_REQUIRES_ACTIVITY_CONTEXT + "IronSource requires an Activity context to load ads.");
            AdError contextError = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT, errorMessage, ERROR_DOMAIN);
            adapter.onAdFailedToLoad(contextError);
            return;
        }

        Activity activity = (Activity) context;

        if (TextUtils.isEmpty(instanceId)) {
            AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid instance ID.", ERROR_DOMAIN);
            adapter.onAdFailedToLoad(loadError);
            return;
        }

        if (!canLoadRewardedVideoInstance(instanceId)) {
            String errorMessage = String
                    .format("An ad is already loading for instance ID: %s", instanceId);
            AdError concurrentError = new AdError(ERROR_AD_ALREADY_LOADED, errorMessage, ERROR_DOMAIN);
            adapter.onAdFailedToLoad(concurrentError);
            return;
        }

        registerISRewardedVideoAdapter(instanceId, new WeakReference<>(adapter));
        IronSource.loadISDemandOnlyRewardedVideo(activity, instanceId);
    }

    public void loadBanner(@NonNull MediationBannerAdConfiguration MediationBannerAdConfiguration,
                           @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback,
                           Context context,
                           String instanceId) {
        ironSourceBannerAd = new IronSourceBannerAd(MediationBannerAdConfiguration, callback);
        registerISBannerAdapter(instanceId, new WeakReference(ironSourceBannerAd));
        ironSourceBannerAd.render();
    }

    private boolean canLoadInterstitialInstance(@NonNull String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter == null) {
            return true;
        }

        IronSourceAdapter ironSourceAdapter = weakAdapter.get();
        return (ironSourceAdapter == null);
    }

    private boolean canLoadRewardedVideoInstance(@NonNull String instanceId) {
        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);
        if (weakAdapter == null) {
            return true;
        }

        IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
        return (ironSourceMediationAdapter == null);
    }



    void showRewardedVideo(@NonNull String instanceId, @NonNull IronSourceMediationAdapter adapter) {
        WeakReference<IronSourceMediationAdapter> adapterReference = availableInstances.get(instanceId);
        if (adapterReference == null || adapterReference.get() == null || !adapter
                .equals(adapterReference.get())) {
            AdError showError = new AdError(ERROR_AD_SHOW_UNAUTHORIZED,
                    "IronSource adapter does not have authority to show this instance.", ERROR_DOMAIN);
            adapter.onAdFailedToShow(showError);
            return;
        }

        // IronSource may call onRewardedVideoAdRewarded() after onRewardedVideoAdClosed() on some
        // rewarded ads. Store the adapter reference so callbacks can be forwarded properly regardless
        // of order.
        currentlyShowingRewardedAdapter = adapterReference;
        IronSource.showISDemandOnlyRewardedVideo(instanceId);
    }

    void showInterstitial(@NonNull String instanceId) {
        IronSource.showISDemandOnlyInterstitial(instanceId);
    }

    private void registerISInterstitialAdapter(@NonNull String instanceId, @NonNull WeakReference<IronSourceAdapter> weakAdapter) {
        IronSourceAdapter ironSourceAdapter = weakAdapter.get();
        if (ironSourceAdapter == null) {
            Log.e(TAG, "IronSource interstitial adapter weak reference has been lost.");
            return;
        }

        availableInterstitialInstances.put(instanceId, weakAdapter);
    }

    private void registerISRewardedVideoAdapter(@NonNull String instanceId, @NonNull WeakReference<IronSourceMediationAdapter> weakAdapter) {
        IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
        if (ironSourceMediationAdapter == null) {
            Log.e(TAG, "IronSource rewarded adapter weak reference has been lost.");
            return;
        }

        availableInstances.put(instanceId, weakAdapter);
    }

    private void registerISBannerAdapter(@NonNull String instanceId, @NonNull WeakReference<IronSourceBannerAd> weakAdapter) {
        IronSourceBannerAd IronSourceBannerAd = weakAdapter.get();
        if (IronSourceBannerAd == null) {
            Log.e(TAG, "IronSource banner adapter weak reference has been lost.");
            return;
        }

        availableBannerInstances.put(instanceId, weakAdapter);
    }


    // region ISDemandOnlyRewardedVideoListener implementation.

    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                ironSourceMediationAdapter.onRewardedVideoAdLoadSuccess(instanceId);
            }
        }
    }

    @Override
    public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                ironSourceMediationAdapter.onRewardedVideoAdLoadFailed(instanceId, ironSourceError);
            }

            availableInstances.remove(instanceId);
        }
    }

    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        if (currentlyShowingRewardedAdapter != null && currentlyShowingRewardedAdapter.get() != null) {
            currentlyShowingRewardedAdapter.get().onRewardedVideoAdOpened(instanceId);
        }
    }

    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        if (currentlyShowingRewardedAdapter != null && currentlyShowingRewardedAdapter.get() != null) {
            currentlyShowingRewardedAdapter.get().onRewardedVideoAdClosed(instanceId);
        }

        availableInstances.remove(instanceId);
    }

    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        if (currentlyShowingRewardedAdapter != null && currentlyShowingRewardedAdapter.get() != null) {
            currentlyShowingRewardedAdapter.get()
                    .onRewardedVideoAdShowFailed(instanceId, ironSourceError);
        }

        availableInstances.remove(instanceId);
    }

    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        if (currentlyShowingRewardedAdapter != null && currentlyShowingRewardedAdapter.get() != null) {
            currentlyShowingRewardedAdapter.get().onRewardedVideoAdClicked(instanceId);
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String instanceId) {
        if (currentlyShowingRewardedAdapter != null && currentlyShowingRewardedAdapter.get() != null) {
            currentlyShowingRewardedAdapter.get().onRewardedVideoAdRewarded(instanceId);
        }
    }


    // region ISDemandOnlyInterstitialListener implementation.

    @Override
    public void onInterstitialAdReady(String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdReady(instanceId);
            }
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdLoadFailed(instanceId, ironSourceError);
            }

            availableInterstitialInstances.remove(instanceId);
        }
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdOpened(instanceId);
            }
        }
    }

    @Override
    public void onInterstitialAdClosed(String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdClosed(instanceId);
            }

            availableInterstitialInstances.remove(instanceId);
        }
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdShowFailed(instanceId, ironSourceError);
            }
            availableInterstitialInstances.remove(instanceId);
        }
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdClicked(instanceId);
            }
        }
    }

    // region ISDemandOnlyBannerListener implementation.

    @Override
    public void onBannerAdLoaded(String instanceId) {
        WeakReference<IronSourceBannerAd> weakAdapter = availableBannerInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceBannerAd ironSourceBannerAd = weakAdapter.get();
            if (ironSourceBannerAd != null) {
                ironSourceBannerAd.onBannerAdLoaded(instanceId);
            }
        }
    }

    @Override
    public void onBannerAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        WeakReference<IronSourceBannerAd> weakAdapter = availableBannerInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceBannerAd ironSourceBannerAd = weakAdapter.get();
            if (ironSourceBannerAd != null) {
                ironSourceBannerAd.onBannerAdLoadFailed(instanceId, ironSourceError);
            }

            if (ironSourceError.getErrorCode() == ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS || ironSourceError.getErrorCode() == ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS) {
            // IronSource banner ad is already Loading or Showing
            } else {
                availableBannerInstances.remove(instanceId);
            }

        }
    }

    @Override
    public void onBannerAdShown(String instanceId) {
        WeakReference<IronSourceBannerAd> weakAdapter = availableBannerInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceBannerAd ironSourceBannerAd = weakAdapter.get();
            if (ironSourceBannerAd != null) {
                ironSourceBannerAd.onBannerAdShown(instanceId);
            }
            for (String otherInstanceInMap : availableBannerInstances.keySet()) {
                if (!otherInstanceInMap.equals(instanceId)){
                    Log.d(TAG, String.format("IronSource Destroy banner ad with instance ID: %s", otherInstanceInMap));
                    IronSource.destroyISDemandOnlyBanner(otherInstanceInMap);
                }
            }
        }
    }

    @Override
    public void onBannerAdClicked(String instanceId) {
        WeakReference<IronSourceBannerAd> weakAdapter = availableBannerInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceBannerAd ironSourceBannerAd = weakAdapter.get();
            if (ironSourceBannerAd != null) {
                ironSourceBannerAd.onBannerAdClicked(instanceId);
            }
        }
    }

    @Override
    public void onBannerAdLeftApplication(String instanceId) {
        WeakReference<IronSourceBannerAd> weakAdapter = availableBannerInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceBannerAd ironSourceBannerAd = weakAdapter.get();
            if (ironSourceBannerAd != null) {
                ironSourceBannerAd.onBannerAdLeftApplication(instanceId);
            }
        }
    }

    // region InitializationCallback implementation.

    interface InitializationCallback {
        void onInitializeSuccess();

        void onInitializeError(@NonNull AdError initializationError);
    }

}
