package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.ads.mediation.tapjoy.rtb.TapjoyRtbInterstitialRenderer;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
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
import com.tapjoy.Tapjoy;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class TapjoyMediationAdapter extends RtbAdapter {

    static final String TAG = TapjoyMediationAdapter.class.getSimpleName();

    static final String SDK_KEY_SERVER_PARAMETER_KEY = "sdkKey";
    static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
    static final String MEDIATION_AGENT = "admob";
    static final String TAPJOY_INTERNAL_ADAPTER_VERSION =
            "1.0.0"; // only used internally for Tapjoy SDK

    /**
     * {@link Adapter} implementation
     */
    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = Tapjoy.getVersion();
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
    }

    @Override
    public void initialize(Context context,
                           final InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: "
                    + "Tapjoy SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> sdkKeys = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String sdkKeyFromServer = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);

            if (!TextUtils.isEmpty(sdkKeyFromServer)) {
                sdkKeys.add(sdkKeyFromServer);
            }
        }

        String sdkKey;
        int count = sdkKeys.size();
        if (count > 0) {
            sdkKey = sdkKeys.iterator().next();

            if (count > 1) {
                String message = String.format("Multiple '%s' entries found: %s. " +
                                "Using '%s' to initialize the IronSource SDK.",
                        SDK_KEY_SERVER_PARAMETER_KEY, sdkKeys.toString(), sdkKey);
                Log.w(TAG, message);
            }
        } else {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: Missing or Invalid SDK key.");
            return;
        }

        Tapjoy.setActivity((Activity) context);

        Hashtable<String, Object> connectFlags = new Hashtable<>();
        // TODO: Get Debug flag from publisher at init time. Currently not possible.
        // connectFlags.put("TJC_OPTION_ENABLE_LOGGING", true);

        TapjoyInitializer.getInstance().initialize((Activity) context, sdkKey, connectFlags,
                new TapjoyInitializer.Listener() {
            @Override
            public void onInitializeSucceeded() {
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void onInitializeFailed(String message) {
                initializationCompleteCallback.onInitializationFailed("Initialization failed: "
                                + message);
            }
        });
    }

    @Override
    public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
        signalCallbacks.onSuccess(Tapjoy.getUserToken());
    }

    @Override
    public void loadInterstitialAd(
            MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
            TapjoyRtbInterstitialRenderer interstitialRenderer =
                    new TapjoyRtbInterstitialRenderer(mediationInterstitialAdConfiguration, mediationAdLoadCallback);
            interstitialRenderer.render();
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
            TapjoyRewardedRenderer rewardedRenderer =
                    new TapjoyRewardedRenderer(mediationRewardedAdConfiguration, mediationAdLoadCallback);
            rewardedRenderer.render();
    }

}
