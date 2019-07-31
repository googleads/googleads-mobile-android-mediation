package com.google.ads.mediation.vungle;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.VungleConsent;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class VungleMediationAdapter extends Adapter
        implements MediationRewardedAd, VungleInitializer.VungleInitializationListener,
        LoadAdCallback, PlayAdCallback {

    public static final String TAG = VungleMediationAdapter.class.getSimpleName();
    private static final String KEY_APP_ID = "appid";

    private AdConfig mAdConfig;
    private String mUserID;
    private String mPlacement;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private static HashMap<String, WeakReference<VungleMediationAdapter>> mPlacementsInUse =
            new HashMap<>();

    private InitializationCompleteCallback mInitializationCallback;
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

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
        String versionString = com.vungle.warren.BuildConfig.VERSION_NAME;
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

        if (VungleInitializer.getInstance().isInitialized()) {
            initializationCompleteCallback.onInitializationSucceeded();
            return;
        }

        if (!(context instanceof Activity)) {
            initializationCompleteCallback.onInitializationFailed("Vungle SDK requires an " +
                    "Activity context to initialize");
            return;
        }

        HashSet<String> appIDs = new HashSet<>();
        for (MediationConfiguration configuration: mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String appIDFromServer = serverParameters.getString(KEY_APP_ID);

            if (!TextUtils.isEmpty(appIDFromServer)) {
                appIDs.add(appIDFromServer);
            }
        }

        int count = appIDs.size();
        if (count > 0) {
            String appID = appIDs.iterator().next();

            if (count > 1) {
                String logMessage = String.format("Multiple '%s' entries found: %s. " +
                        "Using '%s' to initialize the Vungle SDK.",
                        KEY_APP_ID, appIDs.toString(), appID);
                Log.w(TAG, logMessage);
            }

            mInitializationCallback = initializationCompleteCallback;
            VungleInitializer.getInstance().initialize(appID, context.getApplicationContext(),
                    VungleMediationAdapter.this);
        } else {
            initializationCompleteCallback.onInitializationFailed("Initialization failed: " +
                    "Missing or Invalid App ID.");
        }
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {
        mMediationAdLoadCallback = mediationAdLoadCallback;

        Context context = mediationRewardedAdConfiguration.getContext();
        if (!(context instanceof Activity)){
            mediationAdLoadCallback.onFailure(
                    "Vungle SDK requires an Activity context to initialize");
            return;
        }

        Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

        if (mediationExtras != null) {
            mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
        }

        mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacement)) {
            String logMessage = "Failed to load ad from Vungle: Missing or invalid Placement ID.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        if (mPlacementsInUse.containsKey(mPlacement) &&
                mPlacementsInUse.get(mPlacement).get() != null){
            String logMessage = "Only a maximum of one ad can be loaded per placement.";
            Log.w(TAG, logMessage);
            mediationAdLoadCallback.onFailure(logMessage);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

        if (!VungleInitializer.getInstance().isInitialized()) {
            String appID = serverParameters.getString(KEY_APP_ID);
            if (TextUtils.isEmpty(appID)) {
                String logMessage = "Failed to load ad from Vungle: Missing or Invalid App ID.";
                Log.w(TAG, logMessage);
                mediationAdLoadCallback.onFailure(logMessage);
                return;
            }

            VungleInitializer.getInstance().initialize(appID, context.getApplicationContext(),
                    VungleMediationAdapter.this);
        } else {
            Vungle.setIncentivizedFields(mUserID, null, null, null, null);
            mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));

            if (Vungle.canPlayAd(mPlacement)) {
                mMediationRewardedAdCallback =
                        mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
            } else {
                Vungle.loadAd(mPlacement, VungleMediationAdapter.this);
            }
        }
    }

    @Override
    public void onInitializeSuccess() {
        if (VungleConsent.getCurrentVungleConsent() != null) {
            Vungle.updateConsentStatus(VungleConsent.getCurrentVungleConsent(),
                    VungleConsent.getCurrentVungleConsentMessageVersion());
        }

        if (mInitializationCallback != null) {
            mInitializationCallback.onInitializationSucceeded();
        }

        // If mPlacement has a value, then an Ad Request is pending.
        if (!TextUtils.isEmpty(mPlacement)) {
            Vungle.setIncentivizedFields(mUserID, null, null, null, null);
            mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));

            if (Vungle.canPlayAd(mPlacement)) {
                if (mMediationAdLoadCallback != null) {
                    mMediationRewardedAdCallback =
                            mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
                }
            } else {
                Vungle.loadAd(mPlacement, VungleMediationAdapter.this);
            }
        }
    }

    @Override
    public void onInitializeError(String errorMessage) {
        if (mInitializationCallback != null) {
            mInitializationCallback.onInitializationFailed(
                    "Initialization Failed: " + errorMessage);
        }

        // If 'mMediationAdLoadCallback' has a value, then an Ad Request is pending and has failed.
        if (mMediationAdLoadCallback != null) {
            mMediationAdLoadCallback.onFailure("Failed to load ad from Vungle: " + errorMessage);
            mPlacementsInUse.remove(mPlacement);
        }
    }

    @Override
    public void showAd(Context context) {
        if (Vungle.canPlayAd(mPlacement)) {
            Vungle.playAd(mPlacement, mAdConfig, VungleMediationAdapter.this);
        } else {
            if (mMediationRewardedAdCallback != null) {
                mMediationRewardedAdCallback.onAdFailedToShow("Not ready.");
            }
            mPlacementsInUse.remove(mPlacement);
        }
    }

    /**
     * {@link LoadAdCallback} implemenatation from Vungle
     */
    @Override
    public void onAdLoad(final String placementID) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediationAdLoadCallback != null) {
                    mMediationRewardedAdCallback =
                            mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
                }
                mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));
            }
        });
    }

    /**
     * {@link PlayAdCallback} implemenatation from Vungle
     */
    @Override
    public void onAdStart(final String placementID) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdOpened();
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            }
        });
    }

    @Override
    public void onAdEnd(final String placementID,
                        final boolean wasSuccessfulView,
                        final boolean wasCallToActionClicked) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediationRewardedAdCallback != null) {
                    if (wasSuccessfulView) {
                        mMediationRewardedAdCallback.onVideoComplete();
                        mMediationRewardedAdCallback.onUserEarnedReward(
                                new VungleReward("vungle", 1));
                    }
                    if (wasCallToActionClicked) {
                        // Only the call to action button is clickable for Vungle ads. So the
                        // wasCallToActionClicked can be used for tracking clicks.
                        mMediationRewardedAdCallback.reportAdClicked();
                    }
                    mMediationRewardedAdCallback.onAdClosed();
                }
                mPlacementsInUse.remove(placementID);
            }
        });
    }

    // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
    // ad request to Vungle fails, and when an ad fails to play.
    @Override
    public void onError(final String placementID,
                        final Throwable throwable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediationAdLoadCallback != null) {
                    Log.w(TAG, "Failed to load ad from Vungle", throwable);
                    mMediationAdLoadCallback.onFailure(throwable.getLocalizedMessage());
                }

                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdFailedToShow(throwable.getLocalizedMessage());
                }
                mPlacementsInUse.remove(placementID);
            }
        });
    }

    /**
     * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
     */
    private class VungleReward implements RewardItem {
        private final String mType;
        private final int mAmount;

        VungleReward(String type, int amount) {
            mType = type;
            mAmount = amount;
        }

        @Override
        public int getAmount() {
            return mAmount;
        }

        @Override
        public String getType() {
            return mType;
        }
    }
}
