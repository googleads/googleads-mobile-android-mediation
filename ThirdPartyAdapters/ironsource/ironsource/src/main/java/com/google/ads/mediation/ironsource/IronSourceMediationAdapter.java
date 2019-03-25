package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

public class IronSourceMediationAdapter extends Adapter
        implements MediationRewardedAd, ISDemandOnlyRewardedVideoListener,
        IronSourceRewardedAvailabilityListener {

    /**
     * Mediation rewarded ad listener used to forward rewarded ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedAdCallback mMediationRewardedAdCallback;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;

    private static IronSourceRewardedManager mISRewardedManager =
            IronSourceRewardedManager.getInstance();

    /**
     * This is the id of the rewarded video instance requested.
     */
    private String mInstanceID;

    /**
     * MediationRewardedAd implementation.
     */
    @Override
    public VersionInfo getSDKVersionInfo() {
        String sdkVersion = IronSourceUtils.getSDKVersion();
        String splits[] = sdkVersion.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        // Adapter versions have 2 patch versions. Multiply the first patch by 100.
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("IronSource SDK requires " +
                    "an Activity context to initialize");
            return;
        }

        try {
            HashSet<String> appKeys = new HashSet<>();
            for (MediationConfiguration configuration: mediationConfigurations) {
                Bundle serverParameters = configuration.getServerParameters();
                String appKey = serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);

                if (!TextUtils.isEmpty(appKey)) {
                    appKeys.add(appKey);
                }
            }

            int count = appKeys.size();
            if (count > 0) {
                String appKey = appKeys.iterator().next();

                if (count > 1) {
                    String message = String.format("Multiple '%s' entries found: %s. " +
                                    "Using '%s' to initialize the IronSource SDK.",
                            IronSourceAdapterUtils.KEY_APP_KEY, appKeys.toString(), appKey);
                    Log.w(IronSourceAdapterUtils.TAG, message);
                }

                IronSourceAdapterUtils.initIronSourceSDK((Activity) context, appKey,
                        IronSource.AD_UNIT.REWARDED_VIDEO);
                initializationCompleteCallback.onInitializationSucceeded();
            } else {
                initializationCompleteCallback.onInitializationFailed(
                        "Initialization Failed: Missing or Invalid App Key.");
            }
        } catch (Exception e) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: " + e.getMessage());
        }
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                               MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        mInstanceID = serverParameters.getString(IronSourceAdapterUtils.KEY_INSTANCE_ID, "0");

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "IronSource SDK requires an Activity context to initialize";
            Log.w(IronSourceAdapterUtils.TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mMediationAdLoadCallback = mediationAdLoadCallback;
        IronSource.setISDemandOnlyRewardedVideoListener(IronSourceMediationAdapter.this);

        if (IronSourceAdapterUtils.isIronSourceInitialized(IronSource.AD_UNIT.REWARDED_VIDEO)) {
            if (IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceID)) {
                if (mISRewardedManager.isISRewardedAdRegistered(mInstanceID)) {
                    String logMessage = "Failed to request ad from IronSource: " +
                            "Only a maximum of one ad per instance ID can be loaded.";
                    Log.w(IronSourceAdapterUtils.TAG, logMessage);
                    mMediationAdLoadCallback.onFailure(logMessage);
                } else {
                    mMediationRewardedAdCallback = mMediationAdLoadCallback
                            .onSuccess(IronSourceMediationAdapter.this);
                    mISRewardedManager.registerISRewardedAd(mInstanceID,
                            new WeakReference<>(IronSourceMediationAdapter.this));
                }
            } else {
                if (mISRewardedManager.isISRewardedAdLoading(mInstanceID)) {
                    String logMessage = "Failed to request ad from IronSource: " +
                            "A request for the same instance ID is still loading.";
                    Log.w(IronSourceAdapterUtils.TAG, logMessage);
                    mMediationAdLoadCallback.onFailure(logMessage);
                } else {
                    WeakReference<IronSourceRewardedAvailabilityListener> weakListener =
                            new WeakReference<IronSourceRewardedAvailabilityListener>(
                                    IronSourceMediationAdapter.this);
                    mISRewardedManager.addListener(weakListener, mInstanceID);
                }
            }
        } else {
            String appKey = serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                String logMessage = "Initialization failed: Missing or Invalid App Key.";
                Log.e(IronSourceAdapterUtils.TAG, logMessage);
                mMediationAdLoadCallback.onFailure(logMessage);
                return;
            }

            try {
                WeakReference<IronSourceRewardedAvailabilityListener> weakListener =
                        new WeakReference<IronSourceRewardedAvailabilityListener>(
                                IronSourceMediationAdapter.this);
                mISRewardedManager.addListener(weakListener, mInstanceID);

                IronSourceAdapterUtils.initIronSourceSDK((Activity) context,
                        appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            } catch (Exception e) {
                Log.w(IronSourceAdapterUtils.TAG, "Initialization Failed.", e);
                mMediationAdLoadCallback.onFailure(
                        "Initialization failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void showAd(Context context) {
        if (IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceID)) {
            IronSource.setISDemandOnlyRewardedVideoListener(IronSourceMediationAdapter.this);
            IronSource.showISDemandOnlyRewardedVideo(mInstanceID);
        } else {
            // Show ad will only be called if the adapter sends back an ad
            // loaded callback in response to a loadAd request. If for any
            // reason the adapter is not ready to show an ad after sending
            // an ad loaded callback, log a warning.
            final String message = "No ads to show.";
            Log.w(IronSourceAdapterUtils.TAG, message);
            if (mMediationRewardedAdCallback != null){
                IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                    public void run() {
                        mMediationRewardedAdCallback.onAdFailedToShow(message);
                        mISRewardedManager.unregisterISRewardedAd(mInstanceID);
                    }
                });
            }
        }
    }

    /**
     * IronSource RewardedVideoListener implementation.
     */
    @Override
    public void onRewardedVideoAvailabilityChanged(final String instanceId,
                                                   final boolean available) {
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video changed " +
                "availability: %b for instance %s", available, instanceId));

        mISRewardedManager.onRewardedVideoAvailabilityChanged(instanceId, available);
    }

    @Override
    public void onRewardedVideoAdOpened(final String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Rewarded Video opened ad for instance "
                + instanceId);

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdOpened();
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Rewarded Video closed ad for instance "
                + instanceId);

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdClosed();
                    mISRewardedManager.unregisterISRewardedAd(mInstanceID);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String instanceId, final Placement placement) {
        if (placement == null) {
            Log.w(IronSourceAdapterUtils.TAG, "IronSource Placement Error");
            return;
        }

        final IronSourceReward reward = new IronSourceReward(placement);
        Log.d(IronSourceAdapterUtils.TAG, String.format("IronSource Rewarded Video received " +
                        "reward: %d %s, for instance %s.",
                reward.getAmount(), reward.getType(), instanceId));

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onVideoComplete();
                    mMediationRewardedAdCallback.onUserEarnedReward(new IronSourceReward(placement));
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdShowFailed(final String instanceId, IronSourceError ironsourceError) {
        final String message = String.format("IronSource Rewarded Video failed to show " +
                "for instance %s, Error: %s", instanceId, ironsourceError.getErrorMessage());
        Log.w(IronSourceAdapterUtils.TAG, message);

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.onAdFailedToShow(message);
                    mISRewardedManager.unregisterISRewardedAd(instanceId);
                }
            });
        }
    }

    @Override
    public void onRewardedVideoAdClicked(String instanceId, Placement placement) {
        Log.d(IronSourceAdapterUtils.TAG, "IronSource Rewarded Video clicked for instance "
                + instanceId);

        if (mMediationRewardedAdCallback != null) {
            IronSourceAdapterUtils.sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedAdCallback.reportAdClicked();
                }
            });
        }
    }

    /**
     * IronSourceRewardedAvailablilityListener implementation.
     */
    @Override
    public void onRewardedAdAvailable() {
        mMediationRewardedAdCallback =
                mMediationAdLoadCallback.onSuccess(IronSourceMediationAdapter.this);
        mISRewardedManager.registerISRewardedAd(mInstanceID,
                new WeakReference<>(IronSourceMediationAdapter.this));
    }

    @Override
    public void onRewardedAdNotAvailable() {
        mMediationAdLoadCallback.onFailure("Failed to load ad.");
        mISRewardedManager.unregisterISRewardedAd(mInstanceID);
    }

    /**
     * A {@link RewardItem} used to map IronSource reward to Google's reward.
     */
    class IronSourceReward implements RewardItem {

        private final Placement mPlacement;

        IronSourceReward(Placement placement) {
            this.mPlacement = placement;
        }

        @Override
        public String getType() {
            return mPlacement.getRewardName();
        }

        @Override
        public int getAmount() {
            return mPlacement.getRewardAmount();
        }
    }
}
