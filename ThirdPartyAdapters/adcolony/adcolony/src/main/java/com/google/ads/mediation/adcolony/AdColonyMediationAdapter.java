package com.google.ads.mediation.adcolony;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.jirbo.adcolony.AdColonyManager;
import com.jirbo.adcolony.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AdColonyMediationAdapter extends Adapter implements MediationRewardedAd {
    private static final String TAG = AdColonyMediationAdapter.class.getSimpleName();

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
    private MediationRewardedAdCallback mRewardedAdCallback;

    private AdColonyInterstitial mAdColonyInterstitial;

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
        String versionString = AdColony.getSDKVersion();
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]);
        return new VersionInfo(major, minor, micro);
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
            if (zoneIDs != null && zoneIDs.size() > 0){
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
        AdColonyAppOptions appOptions = new AdColonyAppOptions();
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
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        mAdLoadCallback = mediationAdLoadCallback;

        boolean showPrePopup = false;
        boolean showPostPopup = false;

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        Bundle networkExtras = mediationRewardedAdConfiguration.getMediationExtras();

        // Retrieve the appropriate zone for this ad-request.
        ArrayList<String> listFromServerParams =
                AdColonyManager.getInstance().parseZoneList(serverParameters);
        String requestedZone = AdColonyManager
                .getInstance().getZoneFromRequest(listFromServerParams, networkExtras);

        if (AdColonyRewardedEventForwarder.getInstance().isListenerAvailable(requestedZone)) {
            String logMessage = "Failed to load ad from AdColony: " +
                    "Only a maximum of one ad can be loaded per Zone ID.";
            Log.e(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        // Configures the AdColony SDK, which also initializes the SDK if it has not been yet.
        boolean success = AdColonyManager.getInstance()
                .configureAdColony(mediationRewardedAdConfiguration);

        // Check if we have a valid zone and request the ad.
        if (success && !TextUtils.isEmpty(requestedZone)) {
            if (networkExtras != null) {
                showPrePopup = networkExtras.getBoolean("show_pre_popup", false);
                showPostPopup = networkExtras.getBoolean("show_post_popup", false);
            }

            AdColonyAdOptions adOptions = new AdColonyAdOptions()
                    .enableConfirmationDialog(showPrePopup)
                    .enableResultsDialog(showPostPopup);

            AdColonyRewardedEventForwarder.getInstance().addListener(requestedZone,
                    new WeakReference<>(AdColonyMediationAdapter.this));

            AdColony.requestInterstitial(requestedZone,
                    AdColonyRewardedEventForwarder.getInstance(), adOptions);
        } else {
            // Cannot request an ad without a valid zone.
            success = false;
        }

        if (!success) {
            String logMessage = "Failed to request ad from AdColony: Internal Error";
            Log.w(TAG,logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
        }
    }

    @Override
    public void showAd(Context context) {
        if (mAdColonyInterstitial != null) {
            mAdColonyInterstitial.show();
        } else {
            mRewardedAdCallback.onAdFailedToShow("No ad to show.");
        }
    }

    //region AdColony Rewarded Events
    void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
        mAdColonyInterstitial = adColonyInterstitial;
        if (mAdLoadCallback != null) {
            mRewardedAdCallback = mAdLoadCallback.onSuccess(AdColonyMediationAdapter.this);
        }
    }

    void onRequestNotFilled(AdColonyZone zone) {
        if (mAdLoadCallback != null) {
            mAdLoadCallback.onFailure("Failed to load ad from AdColony.");
        }
    }

    void onExpiring(AdColonyInterstitial ad) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
        Log.i(TAG, "AdColony Ad expired. Attempting to load a new ad.");
        mAdColonyInterstitial = null;
        AdColony.requestInterstitial(ad.getZoneID(), AdColonyRewardedEventForwarder.getInstance());
    }

    void onClicked(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
        }
    }

    void onOpened(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.reportAdImpression();
            mRewardedAdCallback.onVideoStart();
        }
    }

    void onLeftApplication(AdColonyInterstitial ad) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    }

    void onClosed(AdColonyInterstitial ad) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
        }
    }

    void onIAPEvent(AdColonyInterstitial ad, String product_id, int engagement_type) {
        // No relevant ad event can be forwarded to the Google Mobile Ads SDK.
    }

    void onReward(com.adcolony.sdk.AdColonyReward adColonyReward) {
        if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();

            if (adColonyReward.success()) {
                AdColonyReward reward = new AdColonyReward(adColonyReward.getRewardName(),
                        adColonyReward.getRewardAmount());
                mRewardedAdCallback.onUserEarnedReward(reward);
            }
        }
    }
    //endregion
}