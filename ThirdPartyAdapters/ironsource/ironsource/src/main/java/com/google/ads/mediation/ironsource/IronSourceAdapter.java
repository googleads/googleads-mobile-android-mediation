package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.ads.mediation.ironsource.IronSourceManager.InitializationCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

import com.google.android.gms.ads.mediation.VersionInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

public class IronSourceAdapter extends Adapter implements MediationInterstitialAd, IronSourceAdapterListener {

    // region Error codes

    // IronSource adapter error domain.
    public static final String ERROR_DOMAIN = "com.google.ads.mediation.ironsource";

    // IronSource SDK error domain.
    public static final String IRONSOURCE_SDK_ERROR_DOMAIN = "com.ironsource.mediationsdk";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                    ERROR_INVALID_SERVER_PARAMETERS,
                    ERROR_REQUIRES_ACTIVITY_CONTEXT,
                    ERROR_AD_ALREADY_LOADED,
                    ERROR_AD_SHOW_UNAUTHORIZED,
            })

    public @interface AdapterError {

    }

    /**
     * Server parameters (e.g. placement ID) are nil.
     */
    public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

    /**
     * IronSource requires an {@link Activity} context to initialize their SDK.
     */
    public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 102;

    /**
     * IronSource can only load 1 ad per IronSource instance ID.
     */
    public static final int ERROR_AD_ALREADY_LOADED = 103;

    /**
     * IronSource adapter does not have authority to show an ad instance.
     */
    public static final int ERROR_AD_SHOW_UNAUTHORIZED = 104;

    // endregion

    /**
     * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile Ads
     * SDK while ad is presented
     */
    private MediationInterstitialAdCallback mediationInterstitialAdCallback;

    /**
     * Mediation listener used to forward interstitial ad events from IronSource SDK to Google Mobile Ads
     * SDK for loading phases of the ad
     */
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
            mediationAdLoadCallback;

    /**
     * This is the id of the interstitial video instance requested.
     */
    private String instanceID;

    @NonNull
    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = IronSourceUtils.getSDKVersion();
        String[] splits = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            if (splits.length >= 4) {
                micro = micro * 100 + Integer.parseInt(splits[3]);
            }

            return new VersionInfo(major, minor, micro);
        }

        String logMessage =
                String.format(
                        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @NonNull
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.ADAPTER_VERSION;
        String[] splits = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            if (splits.length >= 5) {
                micro = micro * 100 + Integer.parseInt(splits[4]);
            }

            return new VersionInfo(major, minor, micro);
        }

        String logMessage =
                String.format(
                        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(@NonNull Context context,
                           @NonNull final InitializationCompleteCallback initializationCompleteCallback,
                           @NonNull List<MediationConfiguration> mediationConfigurations) {

        HashSet<String> appKeys = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String appKeyFromServer = serverParameters.getString(KEY_APP_KEY);

            if (!TextUtils.isEmpty(appKeyFromServer)) {
                appKeys.add(appKeyFromServer);
            }
        }

        int count = appKeys.size();
        if (count <= 0) {
            AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid app key.", ERROR_DOMAIN);
            initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
            return;
        }

        // Having multiple app keys is not considered an error.
        String appKey = appKeys.iterator().next();
        if (count > 1) {
            String message = String
                    .format("Multiple '%s' entries found: %s. Using '%s' to initialize the IronSource SDK.",
                            KEY_APP_KEY, appKeys, appKey);
            Log.w(TAG, message);
        }

        IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
                new IronSourceManager.InitializationCallback() {
                    @Override
                    public void onInitializeSuccess() {
                        initializationCompleteCallback.onInitializationSucceeded();
                    }

                    @Override
                    public void onInitializeError(@NonNull AdError initializationError) {
                        initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
                    }
                });
    }

    @Override
    public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
                                   @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();
        Context context = mediationInterstitialAdConfiguration.getContext();
        String appKey = serverParameters.getString(KEY_APP_KEY);
        this.instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);

        IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
                new InitializationCallback() {
                    @Override
                    public void onInitializeSuccess() {
                        IronSourceAdapter.this.mediationAdLoadCallback = mediationAdLoadCallback;
                        Log.d(TAG,
                                String.format("Loading IronSource interstitial ad with instance ID: %s", instanceID));
                        IronSourceManager.getInstance().loadInterstitial(context, instanceID, IronSourceAdapter.this);
                    }

                    @Override
                    public void onInitializeError(@NonNull AdError initializationError) {
                        Log.e(TAG, initializationError.getMessage());
                        mediationAdLoadCallback.onFailure(initializationError);
                    }
                });
    }

    /**
     * MediationInterstitialAd implementation.
     */

    @Override
    public void showAd(@NonNull Context context) {
        IronSourceManager.getInstance().showInterstitial(instanceID);
    }

    public void onInterstitialAdReady(String instanceId) {
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationAdLoadCallback != null) {
                            mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(IronSourceAdapter.this);
                        }
                    }
                });
    }

    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        final AdError loadError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        String errorMessage = String
                .format("IronSource failed to load Interstitial ad for instance ID: %s. Error: %s",
                        instanceId, loadError.getMessage());
        Log.e(TAG, errorMessage);

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mediationAdLoadCallback.onFailure(loadError);
                    }
                });
    }

    public void onInterstitialAdOpened(String instanceId) {
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationInterstitialAdCallback != null) {
                            mediationInterstitialAdCallback.onAdOpened();
                            mediationInterstitialAdCallback.reportAdImpression();
                        }
                    }
                });
    }

    public void onInterstitialAdClosed(String instanceId) {
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationInterstitialAdCallback != null) {
                            mediationInterstitialAdCallback.onAdClosed();
                        }
                    }
                });
    }

    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        AdError showError = new AdError(ironSourceError.getErrorCode(),
                ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
        String errorMessage = String
                .format("IronSource failed to show interstitial ad for instance ID: %s. Error: %s", instanceId,
                        showError.getMessage());
        Log.e(TAG, errorMessage);

        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationInterstitialAdCallback != null) {
                            mediationInterstitialAdCallback.onAdFailedToShow(showError);
                        }
                    }
                });
    }

    public void onInterstitialAdClicked(String instanceId) {
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationInterstitialAdCallback != null) {
                            mediationInterstitialAdCallback.reportAdClicked();
                        }
                    }
                });
    }

    public void loadBannerAd(@NonNull MediationBannerAdConfiguration MediationBannerAdConfiguration,
                             @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        Bundle serverParameters = MediationBannerAdConfiguration.getServerParameters();
        Context context = MediationBannerAdConfiguration.getContext();
        this.instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
        IronSourceManager.getInstance().loadBanner(MediationBannerAdConfiguration, callback, context, instanceID);
    }

    // region IronSourceAdapterListener implementation.

    @Override
    public void onAdFailedToLoad(@NonNull AdError loadError) {
        Log.e(TAG, loadError.getMessage());
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationAdLoadCallback != null) {
                            mediationAdLoadCallback.onFailure(loadError);
                        }
                    }
                });
    }

    @Override
    public void onAdFailedToShow(@NonNull AdError showError) {
        Log.e(TAG, showError.getMessage());
        IronSourceAdapterUtils.sendEventOnUIThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mediationInterstitialAdCallback != null) {
                            mediationInterstitialAdCallback.onAdFailedToShow(showError);
                        }
                    }
                });
    }

}
