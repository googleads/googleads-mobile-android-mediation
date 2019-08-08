package com.google.ads.mediation.fyber;

import android.content.Context;
import android.util.Log;

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

import java.util.List;

public class FyberMediationAdapter extends Adapter {

    private static final String TAG = FyberMediationAdapter.class.getSimpleName();

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
        Log.w(TAG, "Invalid adapter version format. Returning null");
        return null;
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        return null;  // TODO: Return a VersionInfo based on your SDK's version.
    }

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {
        // TODO: Initialize your SDK. Move onInitializationSucceeded() to the appropriate place
        //  as needed.
        initializationCompleteCallback.onInitializationSucceeded();

    }

    @Override
    public void loadBannerAd(MediationBannerAdConfiguration mediationBannerAdConfiguration,
                             MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                                     mediationAdLoadCallback) {
        //TODO: Banner Ad implementation.
    }

    @Override
    public void loadInterstitialAd(MediationInterstitialAdConfiguration
                                           mediationInterstitialAdConfiguration,
                                   MediationAdLoadCallback<MediationInterstitialAd,
                                           MediationInterstitialAdCallback> mediationAdLoadCallback) {
        //TODO: Interstitial Ad implementation.
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                               MediationAdLoadCallback<MediationRewardedAd,
                                       MediationRewardedAdCallback> mediationAdLoadCallback) {
        //TODO: Rewarded Ad implementation.
    }
}
