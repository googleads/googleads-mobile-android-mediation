package com.google.ads.mediation.applovin;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinIncentivizedAdListener;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.BuildConfig;
import com.applovin.mediation.rtb.AppLovinRtbBannerRenderer;
import com.applovin.mediation.rtb.AppLovinRtbInterstitialRenderer;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.ads.AdFormat;
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
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

public class AppLovinMediationAdapter extends RtbAdapter
        implements MediationRewardedAd, AppLovinAdLoadListener {

    private static final String TAG = AppLovinMediationAdapter.class.getSimpleName();
    private static WeakReference<Context> applicationContextRef;
    private static final String DEFAULT_ZONE = "";
    private static boolean isRtbAd = true;

    // Rewarded video globals.
    public static final HashMap<String, AppLovinIncentivizedInterstitial> INCENTIVIZED_ADS =
            new HashMap<>();
    private static final Object INCENTIVIZED_ADS_LOCK = new Object();

    // Parent objects.
    private AppLovinSdk mSdk;

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;

    // Rewarded Video objects.
    private MediationRewardedAdCallback mRewardedAdCallback;
    private AppLovinIncentivizedInterstitial mIncentivizedInterstitial;
    private String mPlacement;
    private String mZoneId;
    private Bundle mNetworkExtras;
    private MediationRewardedAdConfiguration adConfiguration;
    private AppLovinAd ad;

    @Override
    public void initialize(Context context,
                           InitializationCompleteCallback initializationCompleteCallback,
                           List<MediationConfiguration> mediationConfigurations) {
        log(DEBUG, "Attempting to initialize SDK");

        Context applicationContext = context.getApplicationContext();
        applicationContextRef = new WeakReference<>(applicationContext);

        if (AppLovinUtils.androidManifestHasValidSdkKey(applicationContext)) {
            AppLovinSdk.getInstance(applicationContext).initializeSdk();
        }

        for (MediationConfiguration mediationConfig : mediationConfigurations) {
            AppLovinSdk sdk = AppLovinUtils.retrieveSdk(
                    mediationConfig.getServerParameters(), applicationContextRef.get());
            sdk.initializeSdk();
        }

        initializationCompleteCallback.onInitializationSucceeded();
    }

    @Override
    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        // Adapter versions have 2 patch versions. Multiply the first patch by 100.
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);

        return new VersionInfo(major, minor, micro);
    }

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = AppLovinSdk.VERSION;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int patch = Integer.parseInt(splits[2]);

        return new VersionInfo(major, minor, patch);
    }

    @Override
    public void loadRewardedAd(
            MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            final MediationAdLoadCallback<MediationRewardedAd,
                    MediationRewardedAdCallback> mediationAdLoadCallback) {

        adConfiguration = mediationRewardedAdConfiguration;

        if (mediationRewardedAdConfiguration.getBidResponse().equals("")) {
            isRtbAd = false;
        }

        if (!isRtbAd) {
            synchronized (INCENTIVIZED_ADS_LOCK) {
                Bundle serverParameters = adConfiguration.getServerParameters();
                mPlacement = AppLovinUtils.retrievePlacement(serverParameters);
                mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);
                mSdk = AppLovinUtils.retrieveSdk(serverParameters, adConfiguration.getContext());
                mNetworkExtras = adConfiguration.getMediationExtras();
                mMediationAdLoadCallback = mediationAdLoadCallback;

                String logMessage = String.format("Requesting rewarded video for zone '%s' " +
                        "and placement '%s'.", mZoneId, mPlacement);
                log(DEBUG, logMessage);

                // Check if incentivized ad for zone already exists.
                if (INCENTIVIZED_ADS.containsKey(mZoneId)) {
                    mIncentivizedInterstitial = INCENTIVIZED_ADS.get(mZoneId);
                    mMediationAdLoadCallback.onFailure("Failed");
                    log(ERROR, "Cannot load multiple ads with the same Zone ID. " +
                            "Display one ad before attempting to load another.");
                } else {
                    // If this is a default Zone, create the incentivized ad normally
                    if (DEFAULT_ZONE.equals(mZoneId)) {
                        mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mSdk);
                    }
                    // Otherwise, use the Zones API
                    else {
                        mIncentivizedInterstitial =
                                AppLovinIncentivizedInterstitial.create(mZoneId, mSdk);
                    }
                    INCENTIVIZED_ADS.put(mZoneId, mIncentivizedInterstitial);
                }
            }

            mIncentivizedInterstitial.preload(this);

        } else {
            mMediationAdLoadCallback = mediationAdLoadCallback;
            mNetworkExtras = adConfiguration.getMediationExtras();
            mSdk = AppLovinUtils.retrieveSdk(adConfiguration.getServerParameters(),
                    adConfiguration.getContext());

            // Create rewarded video object
            mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mSdk);
            // Load ad!

            mSdk.getAdService().loadNextAdForAdToken(adConfiguration.getBidResponse(), this);
        }
    }

    @Override
    public void showAd(Context context) {
        mSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(mNetworkExtras));
        String logMessage = String.format("Showing rewarded video for zone '%s', placement '%s'",
                mZoneId, mPlacement);
        log(DEBUG, logMessage);
        final AppLovinIncentivizedAdListener listener =
                new AppLovinIncentivizedAdListener(adConfiguration, mRewardedAdCallback);

        if (!isRtbAd) {
            if (!mIncentivizedInterstitial.isAdReadyToDisplay()) {
                mRewardedAdCallback.onAdFailedToShow("Ad Failed to show");
            } else {
                mIncentivizedInterstitial.show(context, listener, listener, listener, listener);
            }
        } else {
            mIncentivizedInterstitial.show(ad, context, listener, listener, listener, listener);
        }
    }

    @Override
    public void collectSignals(RtbSignalData rtbSignalData, SignalCallbacks signalCallbacks) {
        final MediationConfiguration config = rtbSignalData.getConfiguration();

        // Check if supported ad format
        if (config.getFormat() == AdFormat.NATIVE) {
            handleCollectSignalsFailure("Requested to collect signal for " +
                    "unsupported native ad format. Ignoring...", signalCallbacks);
            return;
        }

        // Check if the publisher provided extra parameters
        if (rtbSignalData.getNetworkExtras() != null) {
            Log.i(TAG, "Extras for signal collection: " + rtbSignalData.getNetworkExtras());
        }

        AppLovinSdk sdk = AppLovinUtils.retrieveSdk(config.getServerParameters(),
                rtbSignalData.getContext());
        String bidToken = sdk.getAdService().getBidToken();

        if (!TextUtils.isEmpty(bidToken)) {
            Log.i(TAG, "Generated bid token: " + bidToken);
            signalCallbacks.onSuccess(bidToken);
        } else {
            handleCollectSignalsFailure("Failed to generate bid token", signalCallbacks);
        }
    }

    private void handleCollectSignalsFailure(String errorMessage, SignalCallbacks signalCallbacks) {
        Log.e(TAG, errorMessage);
        signalCallbacks.onFailure(errorMessage);
    }

    @Override
    public void loadBannerAd(
            MediationBannerAdConfiguration mediationBannerAdConfiguration,
            MediationAdLoadCallback<MediationBannerAd,
                    MediationBannerAdCallback> mediationAdLoadCallback) {

    AppLovinRtbBannerRenderer bannerRenderer =
        new AppLovinRtbBannerRenderer(mediationBannerAdConfiguration, mediationAdLoadCallback);
        bannerRenderer.loadAd();
    }

    @Override
    public void loadInterstitialAd(
            MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd,
                    MediationInterstitialAdCallback> callback) {

        AppLovinRtbInterstitialRenderer interstitialRenderer =
                new AppLovinRtbInterstitialRenderer(mediationInterstitialAdConfiguration, callback);
        interstitialRenderer.loadAd();
    }

    @Override
    public void adReceived(final AppLovinAd appLovinAd) {
        ad = appLovinAd;
        Log.d("INFO", "Rewarded video did load ad: " + ad.getAdIdNumber());
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRewardedAdCallback = mMediationAdLoadCallback
                        .onSuccess(AppLovinMediationAdapter.this);
            }
        });
    }

    @Override
    public void failedToReceiveAd(final int code) {
        log(ERROR, "Rewarded video failed to load with error: " + code);

        if (!isRtbAd) {
            INCENTIVIZED_ADS.remove(mZoneId);
        }
        AppLovinSdkUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediationAdLoadCallback.onFailure("Error");
            }
        });
    }
}
