package com.google.ads.mediation.inmobi;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.inmobi.rtb.InMobiBannerAd;
import com.google.ads.mediation.inmobi.rtb.InMobiInterstitialAd;
import com.google.android.gms.ads.AdSize;
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
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.inmobi.ads.exceptions.SdkNotInitializedException;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.unification.sdk.InitializationStatus;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InMobi Adapter for AdMob Mediation used to load and show rewarded video ads. This class should
 * not be used directly by publishers.
 */
public class InMobiMediationAdapter extends RtbAdapter {

    public static final String TAG = InMobiMediationAdapter.class.getSimpleName();

    // Flag to check whether the InMobi SDK has been initialized or not.
    public static AtomicBoolean isSdkInitialized = new AtomicBoolean(false);

    // Callback listener
    private InMobiBannerAd mInMobiBanner;
    private InMobiInterstitialAd mInMobiInterstitial;
    private InMobiRewardedAd mInMobiRewarded;

    /**
     * {@link Adapter} implementation
     */
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected adapter version format: %s." +
                "Returning 0.0.0 for adapter version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = InMobiSdk.getVersion();
        String splits[] = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage = String.format("Unexpected SDK version format: %s." +
                "Returning 0.0.0 for SDK version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed(
                    "InMobi SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> accountIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            String serverAccountID = configuration.getServerParameters()
                    .getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);

            if (!TextUtils.isEmpty(serverAccountID)) {
                accountIDs.add(serverAccountID);
            }
        }

        int count = accountIDs.size();
        if (count > 0) {
            String accountID = accountIDs.iterator().next();

            if (count > 1) {
                String message = String.format("Multiple '%s' entries found: %s. "
                                + "Using '%s' to initialize the InMobi SDK",
                        InMobiAdapterUtils.KEY_ACCOUNT_ID, accountIDs, accountID);
                Log.w(TAG, message);
            }

            @InitializationStatus String status = InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj());
            isSdkInitialized.set(status.equals(InitializationStatus.SUCCESS));
            if (isSdkInitialized.get()) {
                initializationCompleteCallback.onInitializationSucceeded();
            } else {
                initializationCompleteCallback.onInitializationFailed(status);
            }
        } else {
            String logMessage = "Initialization failed: Missing or invalid Account ID.";
            Log.d(TAG, logMessage);
            initializationCompleteCallback.onInitializationFailed(logMessage);
        }
    }

    @Override
    public void collectSignals(RtbSignalData rtbSignalData, final SignalCallbacks signalCallbacks) {
        MediationConfiguration mediationConfiguration = rtbSignalData.getConfiguration();
        Bundle serverParameters = mediationConfiguration.getServerParameters();

        String accountId = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
        final Context context = rtbSignalData.getContext();
        @InitializationStatus String status = InMobiSdk.init(context, accountId);
        if (!status.equals(InitializationStatus.SUCCESS)) {
            signalCallbacks.onFailure(status);
            return;
        }
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG);
        final long placementId =
                Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID));
        Handler mainHandler = new Handler(context.getMainLooper());

        try {
            switch (mediationConfiguration.getFormat()) {
                case BANNER:
                    final AdSize adSize = rtbSignalData.getAdSize();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mInMobiBanner = new InMobiBannerAd(context, placementId, adSize);
                            mInMobiBanner.collectSignals(signalCallbacks);
                        }
                    });
                    break;
                case INTERSTITIAL:
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mInMobiInterstitial = new InMobiInterstitialAd(context, placementId);
                            mInMobiInterstitial.collectSignals(signalCallbacks);
                        }
                    });
                    break;
                case REWARDED:
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mInMobiRewarded = new InMobiRewardedAd(context, placementId);
                            mInMobiRewarded.collectSignals(signalCallbacks);
                        }
                    });
                    break;
            }
        } catch (SdkNotInitializedException e) {
            signalCallbacks.onFailure(e.getMessage());
            return;
        }
    }

    @Override
    public void loadBannerAd(MediationBannerAdConfiguration adConfiguration,
                             MediationAdLoadCallback<MediationBannerAd,
                                     MediationBannerAdCallback> callback) {
        Bundle serverParameters = adConfiguration.getServerParameters();
        final long placementId =
                Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID));
        try {
            mInMobiBanner = new InMobiBannerAd(adConfiguration.getContext(), placementId,
                    adConfiguration.getAdSize());
            mInMobiBanner.load(adConfiguration, callback);
        } catch (SdkNotInitializedException e) {
            callback.onFailure(e.getMessage());
            return;
        }
    }

    @Override
    public void loadInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
                                   MediationAdLoadCallback<MediationInterstitialAd,
                                           MediationInterstitialAdCallback> mediationAdLoadCallback) {
        Bundle serverParameters = adConfiguration.getServerParameters();
        final long placementId =
                Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID));
        try {
            mInMobiInterstitial = new InMobiInterstitialAd(adConfiguration.getContext(),
                    placementId);
            mInMobiInterstitial.load(adConfiguration, mediationAdLoadCallback);
        } catch (SdkNotInitializedException e) {
            mediationAdLoadCallback.onFailure(e.getMessage());
            return;
        }
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            final MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        final long placementId =
                Long.parseLong(serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID));
        try {
            mInMobiRewarded = new InMobiRewardedAd(mediationRewardedAdConfiguration.getContext(),
                    placementId);
            mInMobiRewarded.load(mediationRewardedAdConfiguration, mediationAdLoadCallback);
        } catch (SdkNotInitializedException e) {
            mediationAdLoadCallback.onFailure(e.getMessage());
            return;
        }
    }

}
