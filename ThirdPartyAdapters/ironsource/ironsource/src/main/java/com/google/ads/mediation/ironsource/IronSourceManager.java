package com.google.ads.mediation.ironsource;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.INSTANCE_STATE;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.ADAPTER_VERSION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.MEDIATION_NAME;

/**
 * A centralized {@link ISDemandOnlyRewardedVideoListener} to forward IronSource ad events
 * to all {@link IronSourceMediationAdapter} instances.
 */
class IronSourceManager implements ISDemandOnlyRewardedVideoListener, ISDemandOnlyInterstitialListener {

    private static final IronSourceManager instance = new IronSourceManager();

    private ConcurrentHashMap<String, WeakReference<IronSourceMediationAdapter>> availableInstances;

    private ConcurrentHashMap<String, WeakReference<IronSourceAdapter>> availableInterstitialInstances;

    static IronSourceManager getInstance() {
        return instance;
    }

    private IronSourceManager() {
        availableInstances = new ConcurrentHashMap<>();
        availableInterstitialInstances = new ConcurrentHashMap<>();
        IronSource.setISDemandOnlyRewardedVideoListener(this);
        IronSource.setISDemandOnlyInterstitialListener(this);
    }

    void initIronSourceSDK(Activity activity, String appKey, List<IronSource.AD_UNIT> adUnits) {
        IronSource.setMediationType(MEDIATION_NAME + ADAPTER_VERSION_NAME);
        if (adUnits.size() > 0) {
            IronSource.initISDemandOnly(activity, appKey, adUnits.toArray(new IronSource.AD_UNIT[adUnits.size()]));
        }
    }

    void loadInterstitial(String instanceId, @NonNull WeakReference<IronSourceAdapter> weakAdapter) {
        if (TextUtils.isEmpty(instanceId) || weakAdapter == null) {
            log("loadInterstitial- instanceId / weakAdapter is null");
            return;
        }

        IronSourceAdapter ironSourceAdapter = weakAdapter.get();
        if (ironSourceAdapter == null) {
            log("loadInterstitial - ironSourceAdapter is null");
            return;
        }

        if (canLoadInterstitialInstance(instanceId)) {
            changeInterstitialInstanceState(ironSourceAdapter, INSTANCE_STATE.LOCKED);
            registerISInterstitialAdapter(instanceId, weakAdapter);
            IronSource.loadISDemandOnlyInterstitial(instanceId);
        } else {
            ironSourceAdapter.onInterstitialAdLoadFailed(instanceId, new IronSourceError(IronSourceError.ERROR_CODE_GENERIC,
                    "interstitial instance already exists, couldn't load another one at the same time!"));
        }
    }

    void loadRewardedVideo(String instanceId, @NonNull WeakReference<IronSourceMediationAdapter> weakAdapter) {

        if (instanceId == null || weakAdapter == null) {
            log("loadRewardedVideo - instanceId / weakAdapter is null");
            return;
        }

        IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
        if (ironSourceMediationAdapter == null) {
            log("loadRewardedVideo - ironSourceMediationAdapter is null");
            return;
        }

        if (canLoadRewardedVideoInstance(instanceId)) {
            changeRewardedInstanceState(ironSourceMediationAdapter, INSTANCE_STATE.LOCKED);
            registerISRewardedVideoAdapter(instanceId, weakAdapter);
            IronSource.loadISDemandOnlyRewardedVideo(instanceId);
        } else {
            ironSourceMediationAdapter.onRewardedVideoAdLoadFailed(instanceId, new IronSourceError(IronSourceError.ERROR_CODE_GENERIC,
                    "instance already exists, couldn't load another one in the same time!"));
        }
    }


    private boolean canLoadInterstitialInstance(String instanceId) {
        if (!isIsInterstitialAdapterRegistered(instanceId)) {
            return true;
        }

        if (isRegisteredInterstitialAdapterCanLoad(instanceId)) {
            return true;
        }
        return false;

    }

    private boolean canLoadRewardedVideoInstance(String instanceId) {

        if (!isISRewardedVideoAdapterRegistered(instanceId)) {
            return true;
        }
        if (isRegisteredRewardedVideoAdapterCanLoad(instanceId)) {
            return true;
        }
        return false;
    }

    private boolean isRegisteredInterstitialAdapterCanLoad(String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter == null) {
            return true;
        }
        IronSourceAdapter ironSourceAdapter = weakAdapter.get();
        if (ironSourceAdapter == null) {
            return true;
        }
        if (!ironSourceAdapter.getInstanceState().equals(INSTANCE_STATE.CAN_LOAD)) {
            return false;
        }
        return true;
    }

    private boolean isRegisteredRewardedVideoAdapterCanLoad(String instanceId) {
        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);
        if (weakAdapter == null) {
            return true;
        }
        IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
        if (ironSourceMediationAdapter == null) {
            return true;
        }
        if (!ironSourceMediationAdapter.getInstanceState().equals(INSTANCE_STATE.CAN_LOAD)) {
            return false;
        }
        return true;
    }

    void showRewardedVideo(String instanceId) {
        IronSource.showISDemandOnlyRewardedVideo(instanceId);
    }

    void showInterstitial(String instanceId) {
        IronSource.showISDemandOnlyInterstitial(instanceId);
    }

    private void registerISInterstitialAdapter(@NonNull String instanceId,
                                               @NonNull WeakReference<IronSourceAdapter> weakAdapter) {
        if (weakAdapter == null) {
            log("registerISInterstitialAdapter - weakAdapter is null");
            return;
        }
        IronSourceAdapter ironSourceAdapter = weakAdapter.get();
        if (ironSourceAdapter == null) {
            log("registerISInterstitialAdapter - ironSourceMediationAdapter is null");
            return;
        }
        availableInterstitialInstances.put(instanceId, weakAdapter);
    }

    private void registerISRewardedVideoAdapter(@NonNull String instanceId,
                                                @NonNull WeakReference<IronSourceMediationAdapter> weakAdapter) {
        if (weakAdapter == null) {
            log("registerISRewardedVideoAdapter - weakAdapter is null");
            return;
        }

        IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();

        if (ironSourceMediationAdapter == null) {
            log("registerISRewardedVideoAdapter - ironSourceMediationAdapter is null");
            return;
        }

        availableInstances.put(instanceId, weakAdapter);
    }

    private boolean isIsInterstitialAdapterRegistered(@NonNull String instanceId) {
        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isISRewardedVideoAdapterRegistered(@NonNull String instanceId) {
        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);
        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRewardedVideoAdLoadSuccess(String instanceId) {
        log(String.format("IronSourceManager got RV Load success for instance %s", instanceId));

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
        log(String.format("IronSourceManager got RV Load failed for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                changeRewardedInstanceState(ironSourceMediationAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceMediationAdapter.onRewardedVideoAdLoadFailed(instanceId, ironSourceError);
            }
        }
    }

    private void changeRewardedInstanceState(IronSourceMediationAdapter ironSourceMediationAdapter,
                                     INSTANCE_STATE newState) {

        if (ironSourceMediationAdapter == null) {
            log("changeRewardedInstanceState - IronSourceMediationAdapter is null");
            return;
        }

        log(String.format("IronSourceManager change state to %s", newState));
        ironSourceMediationAdapter.setInstanceState(newState);

    }

    private void changeInterstitialInstanceState(IronSourceAdapter ironSourceAdapter,
                                                INSTANCE_STATE newState) {
        if (ironSourceAdapter == null) {
            log("changeInterstitialInstanceState - IronSourceAdapter is null");
            return;
        }

        log(String.format("IronSourceManager change state to %s", newState));
        ironSourceAdapter.setInstanceState(newState);
    }

    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        log(String.format("IronSourceManager got RV ad opened for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                ironSourceMediationAdapter.onRewardedVideoAdOpened(instanceId);
            }
        }
    }

    private void log(String stringToLoad) {
        Log.d(IronSourceAdapterUtils.TAG, stringToLoad);
    }

    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        log(String.format("IronSourceManager got RV ad closed for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                changeRewardedInstanceState(ironSourceMediationAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceMediationAdapter.onRewardedVideoAdClosed(instanceId);
            }
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        log(String.format("IronSourceManager got RV show failed for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                changeRewardedInstanceState(ironSourceMediationAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceMediationAdapter.onRewardedVideoAdShowFailed(instanceId, ironSourceError);
            }
        }
    }

    @Override
    public void onRewardedVideoAdClicked(String instanceId) {
        log(String.format("IronSourceManager got RV ad clicked for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                ironSourceMediationAdapter.onRewardedVideoAdClicked(instanceId);
            }
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String instanceId) {
        log(String.format("IronSourceManager got RV ad rewarded for instance %s", instanceId));

        WeakReference<IronSourceMediationAdapter> weakAdapter = availableInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceMediationAdapter ironSourceMediationAdapter = weakAdapter.get();
            if (ironSourceMediationAdapter != null) {
                ironSourceMediationAdapter.onRewardedVideoAdRewarded(instanceId);
            }
        }
    }

    @Override
    public void onInterstitialAdReady(String instanceId) {
        log(String.format("IronSourceManager got interstitial Load success for instance %s", instanceId));

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
        log(String.format("IronSourceManager got interstitial Load failed for instance %s", instanceId));

        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                changeInterstitialInstanceState(ironSourceAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceAdapter.onInterstitialAdLoadFailed(instanceId, ironSourceError);

            }

        }

    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
        log(String.format("IronSourceManager got interstitial ad opened for instance %s", instanceId));

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
        log(String.format("IronSourceManager got interstitial ad closed for instance %s", instanceId));

        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                changeInterstitialInstanceState(ironSourceAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceAdapter.onInterstitialAdClosed(instanceId);
            }
        }

    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        log(String.format("IronSourceManager got interstitial show failed for instance %s", instanceId));

        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                changeInterstitialInstanceState(ironSourceAdapter,
                        IronSourceMediationAdapter.INSTANCE_STATE.CAN_LOAD);
                ironSourceAdapter.onInterstitialAdShowFailed(instanceId, ironSourceError);
            }
        }

    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        log(String.format("IronSourceManager got interstitial ad clicked for instance %s", instanceId));

        WeakReference<IronSourceAdapter> weakAdapter = availableInterstitialInstances.get(instanceId);

        if (weakAdapter != null) {
            IronSourceAdapter ironSourceAdapter = weakAdapter.get();
            if (ironSourceAdapter != null) {
                ironSourceAdapter.onInterstitialAdClicked(instanceId);
            }
        }
    }
}
