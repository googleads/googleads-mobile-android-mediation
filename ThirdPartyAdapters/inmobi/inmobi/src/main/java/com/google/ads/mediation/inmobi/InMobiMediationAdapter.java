package com.google.ads.mediation.inmobi;

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
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InMobi Adapter for AdMob Mediation used to load and show rewarded video ads. This class should
 * not be used directly by publishers.
 */
public class InMobiMediationAdapter extends Adapter implements MediationRewardedAd {

    private static final String TAG = InMobiMediationAdapter.class.getSimpleName();

    // Flag to check whether the InMobi SDK has been initialized or not.
    private static AtomicBoolean isSdkInitialized = new AtomicBoolean(false);

    // Callback listener
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

    private InMobiInterstitial mInMobiRewardedAd;

    private static HashMap<Long, WeakReference<InMobiMediationAdapter>> mPlacementsInUse =
            new HashMap<>();

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
        String versionString = InMobiSdk.getVersion();
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
            initializationCompleteCallback.onInitializationFailed(
                    "InMobi SDK requires an Activity context to initialize");
            return;
        }

        HashSet<String> accountIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            String serverAccountID =configuration.getServerParameters()
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

            InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj());
            isSdkInitialized.set(true);
            initializationCompleteCallback.onInitializationSucceeded();
        } else {
            String logMessage = "Initialization failed: Missing or invalid Account ID.";
            Log.d(TAG, logMessage);
            initializationCompleteCallback.onInitializationFailed(logMessage);
        }
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            final MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        Context context = mediationRewardedAdConfiguration.getContext();

        if (!(context instanceof Activity)) {
            String logMessage = "Failed to load ad from InMobi: "
                    + "InMobi SDK requires an Activity context to load ads.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        Bundle extras = mediationRewardedAdConfiguration.getMediationExtras();

        if (!isSdkInitialized.get()) {
            String accountID = serverParameters.getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);
            if (TextUtils.isEmpty(accountID)){
                String logMessage =
                        "Failed to load ad from InMobi: Missing or Invalid Account ID.";
                Log.w(TAG, logMessage);
                mediationAdLoadCallback.onFailure(logMessage);
                return;
            }

            InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj());
            isSdkInitialized.set(true);
        }

        String placementString =
                serverParameters.getString(InMobiAdapterUtils.KEY_PLACEMENT_ID);
        if (TextUtils.isEmpty(placementString)) {
            String logMessage = "Failed to load ad from InMobi: Missing or Invalid Placement ID.";
            Log.e(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        final long placementID;
        try {
            placementID = Long.parseLong(placementString.trim());
        } catch (NumberFormatException ex) {
            String logMessage = "Failed to load ad from InMobi: Invalid Placement ID.";
            Log.w(TAG, logMessage, ex);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        if (mPlacementsInUse.containsKey(placementID)
                && mPlacementsInUse.get(placementID).get() != null) {
            String logMessage = "Failed to load ad from InMobi: "
                    + "An ad has already been requested for placement ID: " + placementID;
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mPlacementsInUse.put(placementID, new WeakReference<>(InMobiMediationAdapter.this));
        mInMobiRewardedAd = new InMobiInterstitial(context, placementID,
                new InterstitialAdEventListener() {

            @Override
            public void onRewardsUnlocked(InMobiInterstitial inMobiInterstitial,
                                          Map<Object, Object> rewards) {
                String rewardKey = "";
                String rewardValue = "";

                Log.d(TAG, "InMobi RewardedVideo onRewardsUnlocked.");
                if (rewards != null) {
                    for (Object reward : rewards.keySet()) {
                        rewardKey = reward.toString();
                        rewardValue = rewards.get(rewardKey).toString();
                    }
                }

                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onVideoComplete();
                    mMediationRewardedAdCallback.onUserEarnedReward(
                            new InMobiReward(rewardKey, rewardValue));
                }
            }

            @Override
            public void onAdDisplayFailed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdDisplayFailed");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdFailedToShow("Internal Error.");
                }
                mPlacementsInUse.remove(placementID);
            }

            @Override
            public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdWillDisplay");
            }

            @Override
            public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdDisplayed");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdOpened();
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            }

            @Override
            public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdDismissed");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdClosed();
                }
                mPlacementsInUse.remove(placementID);
            }

            @Override
            public void onAdClicked(InMobiInterstitial inMobiInterstitial,
                                    Map<Object, Object> map) {
                Log.d(TAG, "onAdClicked");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.reportAdClicked();
                }
            }

            @Override
            public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onAdLoadSucceeded");
                mMediationRewardedAdCallback =
                        mediationAdLoadCallback.onSuccess(InMobiMediationAdapter.this);
            }

            @Override
            public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                       InMobiAdRequestStatus inMobiAdRequestStatus) {
                String logMessage = "Failed to load ad from InMobi: "
                        + inMobiAdRequestStatus.getMessage();
                Log.w(TAG, logMessage);
                mediationAdLoadCallback.onFailure(logMessage);
                mPlacementsInUse.remove(placementID);
            }

            @Override
            public void onAdReceived(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "InMobi Ad server responded with an Ad.");
            }

            @Override
            public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                Log.d(TAG, "onUserLeftApplication");
            }
        });

        HashMap<String, String> paramMap =
                InMobiAdapterUtils.createInMobiParameterMap(mediationRewardedAdConfiguration);
        mInMobiRewardedAd.setExtras(paramMap);
        InMobiAdapterUtils.setGlobalTargeting(mediationRewardedAdConfiguration, extras);

        mInMobiRewardedAd.load();
    }

    @Override
    public void showAd(Context context) {
        if (mInMobiRewardedAd.isReady()) {
            mInMobiRewardedAd.show();
        } else if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.onAdFailedToShow("Ad not ready yet.");
        }
    }

    private class InMobiReward implements RewardItem {

        private String mKey;
        private String mValue;

        InMobiReward(String key, String value) {
            this.mKey = key;
            this.mValue = value;
        }

        @Override
        public String getType() {
            return mKey;
        }

        @Override
        public int getAmount() {
            if (!TextUtils.isEmpty(mValue)) {
                try {
                    return Integer.parseInt(mValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Reward value should be of type integer: " + e.getMessage());
                    return 0;
                }
            }
            return 0;
        }
    }

}
