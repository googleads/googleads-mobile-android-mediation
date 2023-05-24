package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.ADAPTER_VERSION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.MEDIATION_NAME;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.VersionInfo;
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
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IronSourceMediationAdapter extends Adapter {

    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);

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

    // region Error codes
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

    /**
     * Banner size mismatch.
     */
    public static final int ERROR_BANNER_SIZE_MISMATCH = 105;

    // endregion

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
    public void initialize(
            @NonNull Context context,
            @NonNull final InitializationCompleteCallback initializationCompleteCallback,
            @NonNull List<MediationConfiguration> mediationConfigurations) {

        if (isInitialized.get()) {
            initializationCompleteCallback.onInitializationSucceeded();
            return;
        }

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
            AdError initializationError =
                    new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid app key.", ERROR_DOMAIN);
            initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
            return;
        }

        // Having multiple app keys is not considered an error.
        String appKey = appKeys.iterator().next();

        if (TextUtils.isEmpty(appKey)) {
            AdError initializationError =
                    new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid app key.", ERROR_DOMAIN);
            initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
            return;
        }

        if (count > 1) {
            String message =
                    String.format(
                            "Multiple '%s' entries found: %s. Using '%s' to initialize the IronSource SDK.",
                            KEY_APP_KEY, appKeys, appKey);
            Log.w(TAG, message);
        }

        IronSource.setMediationType(MEDIATION_NAME + ADAPTER_VERSION_NAME);
        Log.d(TAG, "Initializing IronSource SDK with app key: " + appKey);
        IronSource.initISDemandOnly(
                context,
                appKey,
                IronSource.AD_UNIT.INTERSTITIAL,
                IronSource.AD_UNIT.REWARDED_VIDEO,
                IronSource.AD_UNIT.BANNER);
        isInitialized.set(true);
        initializationCompleteCallback.onInitializationSucceeded();

        IronSource.setISDemandOnlyInterstitialListener(IronSourceInterstitialAd.getIronSourceInterstitialListener());
        IronSource.setISDemandOnlyRewardedVideoListener(IronSourceRewardedAd.getIronSourceRewardedListener());
    }

    /**
     * Load Rewarded Video implementation.
     */
    @Override
    public void loadRewardedAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
                    mediationAdLoadCallback) {

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        Context context = mediationRewardedAdConfiguration.getContext();
        String instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
        IronSourceRewardedAd ironSourceRewardedAd =
                new IronSourceRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);

        if (isInitialized.get()) {
            ironSourceRewardedAd.loadRewardedVideo(context, instanceID);
        }
    }

    /**
     * Load Rewarded Video Interstitial implementation.
     */
    @Override
    public void loadRewardedInterstitialAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            @NonNull
                    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
                    mediationAdLoadCallback) {
        // IronSource Rewarded Interstitial ads use the same Rewarded Video API.
        Log.d(
                TAG,
                "IronSource adapter was asked to load a rewarded interstitial ad. "
                        + "Using the rewarded ad request flow to load the ad to attempt to load a "
                        + "rewarded interstitial ad from IronSource.");
        loadRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);
    }

    /**
     * Load Interstitial implementation.
     */
    @Override
    public void loadInterstitialAd(
            @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            @NonNull
                    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
                    mediationAdLoadCallback) {
        Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();
        Context context = mediationInterstitialAdConfiguration.getContext();
        String instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
        IronSourceInterstitialAd ironSourceInterstitialAd =
                new IronSourceInterstitialAd(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
        if (isInitialized.get()) {
            ironSourceInterstitialAd.loadInterstitial(context, instanceID, ironSourceInterstitialAd);
        }
    }

    /**
     * Load Banner implementation.
     */
    @Override
    public void loadBannerAd(
            @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
            @NonNull
                    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                    mediationAdLoadCallback) {
        Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
        Context context = mediationBannerAdConfiguration.getContext();
        String instanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
        IronSourceBannerAd ironSourceBannerAd =
                new IronSourceBannerAd(mediationAdLoadCallback);
        if (isInitialized.get()) {
            ironSourceBannerAd.loadBanner(
                    context, instanceID, ironSourceBannerAd, mediationBannerAdConfiguration);
        }
    }
}
