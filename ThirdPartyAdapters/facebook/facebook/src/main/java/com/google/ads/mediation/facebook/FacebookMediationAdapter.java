package com.google.ads.mediation.facebook;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.AdSettings;
import com.facebook.ads.BidderTokenProvider;
import com.google.ads.mediation.facebook.rtb.FacebookRtbBannerAd;
import com.google.ads.mediation.facebook.rtb.FacebookRtbInterstitialAd;
import com.google.ads.mediation.facebook.rtb.FacebookRtbNativeAd;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;

import java.util.ArrayList;
import java.util.List;

public class FacebookMediationAdapter extends RtbAdapter {

    public static final String TAG = FacebookAdapter.class.getSimpleName();

    private FacebookRtbBannerAd banner;
    private FacebookRtbInterstitialAd interstitial;
    private FacebookRtbNativeAd nativeAd;
    private FacebookRewardedAd rewardedAd;

    public static final String PLACEMENT_PARAMETER = "pubid";
    public static final String RTB_PLACEMENT_PARAMETER = "placement_id";

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
        String versionString = com.facebook.ads.BuildConfig.VERSION_NAME;
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
    public void initialize(final Context context,
            final InitializationCompleteCallback initializationCompleteCallback,
            List<MediationConfiguration> mediationConfigurations) {

        if (context == null) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: Context is null.");
            return;
        }

        ArrayList<String> placements = new ArrayList<>();
        for (MediationConfiguration adConfiguration : mediationConfigurations) {
            Bundle serverParameters = adConfiguration.getServerParameters();

            String placementID = getPlacementID(serverParameters);
            if (!TextUtils.isEmpty(placementID)) {
                placements.add(placementID);
            }
        }

        if (placements.isEmpty()) {
            initializationCompleteCallback.onInitializationFailed(
                    "Initialization failed: No placement IDs found");
            return;
        }

        FacebookInitializer.getInstance().initialize(context, placements,
                new FacebookInitializer.Listener() {
            @Override
            public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
            }

            @Override
            public void onInitializeError(String message) {
                initializationCompleteCallback.onInitializationFailed(
                                "Initialization failed: " + message);
            }
        });
    }

    @Override
    public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
        String token = BidderTokenProvider.getBidderToken(rtbSignalData.getContext());
        signalCallbacks.onSuccess(token);
    }


    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
                    mediationAdLoadCallback) {
        rewardedAd = new FacebookRewardedAd(mediationRewardedAdConfiguration,
                mediationAdLoadCallback);
        rewardedAd.render();
    }

    @Override
    public void loadBannerAd(MediationBannerAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                    mediationAdLoadCallback) {
        banner = new FacebookRtbBannerAd(adConfiguration, mediationAdLoadCallback);
        banner.render();
    }

    @Override
    public void loadInterstitialAd(MediationInterstitialAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
                    mediationAdLoadCallback) {
        interstitial = new FacebookRtbInterstitialAd(adConfiguration, mediationAdLoadCallback);
        interstitial.render();
    }

    @Override
    public void loadNativeAd(MediationNativeAdConfiguration mediationNativeAdConfiguration,
            MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
                    mediationAdLoadCallback) {
        nativeAd = new FacebookRtbNativeAd(mediationNativeAdConfiguration, mediationAdLoadCallback);
        nativeAd.render();
    }

    /**
     * Gets the Facebook placement ID.
     */
    public static @Nullable String getPlacementID(@NonNull Bundle serverParameters) {
        // Open bidding uses a different key for Placement ID than non-open bidding. Try the open
        // bidding key first.
        String placementId = serverParameters.getString(RTB_PLACEMENT_PARAMETER);
        if (placementId == null) {
            // Fall back to checking the non-open bidding key.
            placementId = serverParameters.getString(PLACEMENT_PARAMETER);
        }
        return placementId;
    }

    /**
     * Sets the Facebook mixed audience settings.
     */
    public static void setMixedAudience(@NonNull MediationAdConfiguration mediationAdConfiguration) {
        if (mediationAdConfiguration.taggedForChildDirectedTreatment() ==
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            AdSettings.setMixedAudience(true);
        } else if (mediationAdConfiguration.taggedForChildDirectedTreatment() ==
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
            AdSettings.setMixedAudience(false);
        }
    }
}
