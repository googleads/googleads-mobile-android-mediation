package com.google.ads.mediation.adcolony;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyCustomMessage;
import com.adcolony.sdk.AdColonyCustomMessageListener;
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
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AdColonyMediationAdapter extends RtbAdapter {
    public static final String TAG = AdColonyMediationAdapter.class.getSimpleName();
    private static AdColonyAppOptions appOptions = new AdColonyAppOptions();
    private static HashMap<String, String> bidResponseDetailsHashMap = new HashMap<>();

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
        String sdkVersion = AdColony.getSDKVersion();
        String splits[] = sdkVersion.split("\\.");

        if (splits.length == 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }
        Log.w(TAG,"Unexpected SDK version format, Returning 0.0.0 for SDK version");
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("AdColony SDK requires an " +
                    "Activity context to initialize");
            return;
        }

        HashSet<String> appIDs = new HashSet<>();
        ArrayList<String> zoneList = new ArrayList<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String appIDFromServer = serverParameters.getString(AdColonyAdapterUtils.KEY_APP_ID);

            if (!TextUtils.isEmpty(appIDFromServer)) {
                appIDs.add(appIDFromServer);
            }

            // We need to include zone IDs from non-rewarded ads to configure the
            // AdColony SDK and avoid issues with Interstitial Ads.
            ArrayList<String> zoneIDs = AdColonyManager.getInstance()
                    .parseZoneList(serverParameters);
            if (zoneIDs != null && zoneIDs.size() > 0) {
                zoneList.addAll(zoneIDs);
            }
        }

        String appID;
        int count = appIDs.size();
        if (count > 0) {
            appID = appIDs.iterator().next();

            if (count > 1) {
                String logMessage = String.format("Multiple '%s' entries found: %s. " +
                                "Using '%s' to initialize the AdColony SDK.",
                        AdColonyAdapterUtils.KEY_APP_ID, appIDs.toString(), appID);
                Log.w(TAG, logMessage);
            }
        } else {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
                    "Missing or Invalid App ID.");
            return;
        }

        if (zoneList.isEmpty()) {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
                    "No zones provided to initialize the AdColony SDK.");
            return;
        }

        // Always set mediation network info.
        appOptions.setMediationNetwork(AdColonyAppOptions.ADMOB, BuildConfig.VERSION_NAME);
        boolean success = AdColony.configure((Activity) context, appOptions, appID,
                zoneList.toArray(new String[0]));

        if (success) {
            initializationCompleteCallback.onInitializationSucceeded();
        } else {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: " +
                    "Internal Error on Configuration");
        }
    }

    @Override
    public void loadInterstitialAd(
            MediationInterstitialAdConfiguration interstitialAdConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd,
                    MediationInterstitialAdCallback> mediationAdLoadCallback) {
        String requestedZone =
                interstitialAdConfiguration.getServerParameters().getString(AdColonyAdapterUtils.KEY_ZONE_ID);
        AdColonyInterstitialRenderer interstitialAd =
                new AdColonyInterstitialRenderer(requestedZone);
        interstitialAd.requestInterstitial(mediationAdLoadCallback);
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration rewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {
        AdColonyRewardedRenderer rewardedAd =
                new AdColonyRewardedRenderer(rewardedAdConfiguration, mediationAdLoadCallback);
        rewardedAd.render();
    }

    public static AdColonyAppOptions getAppOptions() {
        return appOptions;
    }


    @Override
    public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
        String zone, signals = "";
        AdColony.addCustomMessageListener(new AdColonyCustomMessageListener() {
            @Override
            public void onAdColonyCustomMessage(AdColonyCustomMessage adColonyCustomMessage) {

                try {
                    JSONObject jsonObject = new JSONObject(adColonyCustomMessage.getMessage());
                    String zoneID = jsonObject.getString("zone");
                    bidResponseDetailsHashMap.put(zoneID, adColonyCustomMessage.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, "bid");
        if (bidResponseDetailsHashMap != null && bidResponseDetailsHashMap.size() > 0) {
            Bundle serverParameters = rtbSignalData.getConfiguration().getServerParameters();
            zone = serverParameters.getString(AdColonyAdapterUtils.KEY_ZONE_ID);
            signals = bidResponseDetailsHashMap.get(zone);
        }
        signalCallbacks.onSuccess(signals);
    }
    //endregion
}
