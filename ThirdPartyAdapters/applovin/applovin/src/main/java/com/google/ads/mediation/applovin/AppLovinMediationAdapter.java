package com.google.ads.mediation.applovin;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinIncentivizedAdListener;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.mediation.BuildConfig;
import com.applovin.mediation.rtb.AppLovinRtbBannerRenderer;
import com.applovin.mediation.rtb.AppLovinRtbInterstitialRenderer;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.android.gms.ads.AdError;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AppLovinMediationAdapter extends RtbAdapter
    implements MediationRewardedAd, AppLovinAdLoadListener {

  private static final String DEFAULT_ZONE = "";
  private static boolean isRtbAd = true;

  // AppLovin SDK settings.
  public static AppLovinSdkSettings appLovinSdkSettings;

  // AppLovin open-bidding banner ad renderer.
  private AppLovinRtbBannerRenderer mRtbBannerRenderer;

  // AppLovin open-bidding interstitial ad renderer.
  private AppLovinRtbInterstitialRenderer mRtbInterstitialRenderer;

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
  private String mZoneId;
  private Bundle mNetworkExtras;
  private MediationRewardedAdConfiguration adConfiguration;
  private AppLovinAd ad;

  /**
   * Applovin adapter errors.
   */
  // AppLovin adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.applovin";

  // AppLovin SDK error domain.
  public static final String APPLOVIN_SDK_ERROR_DOMAIN = "com.applovin.sdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_EMPTY_BID_TOKEN,
          ERROR_AD_ALREADY_REQUESTED,
          ERROR_PRESENTATON_AD_NOT_READY,
          ERROR_AD_FORMAT_UNSUPPORTED,
          ERROR_INVALID_SERVER_PARAMETERS
      })
  public @interface AdapterError {

  }

  /**
   * Banner size mismatch.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 101;

  /**
   * AppLovin bid token is empty.
   */
  public static final int ERROR_EMPTY_BID_TOKEN = 104;

  /**
   * Requested multiple ads for the same zone. AppLovin can only load 1 ad at a time per zone.
   */
  public static final int ERROR_AD_ALREADY_REQUESTED = 105;

  /**
   * Ad is not ready to display.
   */
  public static final int ERROR_PRESENTATON_AD_NOT_READY = 106;

  /**
   * Adapter does not support the ad format being requested.
   */
  public static final int ERROR_AD_FORMAT_UNSUPPORTED = 108;

  /**
   * Invalid server parameters (e.g. SDK key is null).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 110;

  @NonNull
  public static AppLovinSdkSettings getSdkSettings(@NonNull Context context) {
    if (appLovinSdkSettings == null) {
      appLovinSdkSettings = new AppLovinSdkSettings(context);
    }
    return appLovinSdkSettings;
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    final HashSet<String> sdkKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String sdkKey = configuration.getServerParameters().getString(ServerParameterKeys.SDK_KEY);
      if (!TextUtils.isEmpty(sdkKey)) {
        sdkKeys.add(sdkKey);
      }
    }

    // Include the SDK key declared in the AndroidManifest.xml file.
    String manifestSdkKey = AppLovinUtils.retrieveSdkKey(context, null);
    if (!TextUtils.isEmpty(manifestSdkKey)) {
      sdkKeys.add(manifestSdkKey);
    }

    if (sdkKeys.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid SDK Key.", ERROR_DOMAIN);
      log(WARN, error.getMessage());
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    // Keep track of the SDK keys that were used to initialize the AppLovin SDK. Once all of them
    // have been initialized, then the completion callback is invoked.
    final HashSet<String> initializedSdkKeys = new HashSet<>();

    for (String sdkKey : sdkKeys) {
      AppLovinInitializer.getInstance()
          .initialize(context, sdkKey, new OnInitializeSuccessListener() {
            @Override
            public void onInitializeSuccess(@NonNull String sdkKey) {
              initializedSdkKeys.add(sdkKey);
              if (initializedSdkKeys.equals(sdkKeys)) {
                initializationCompleteCallback.onInitializationSucceeded();
              }
            }
          });
    }
  }

  @Override
  @NonNull
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    log(WARN, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String versionString = AppLovinSdk.VERSION;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int patch = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, patch);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    log(WARN, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    adConfiguration = mediationRewardedAdConfiguration;
    final Context context = adConfiguration.getContext();

    if (mediationRewardedAdConfiguration.getBidResponse().equals("")) {
      isRtbAd = false;
    }

    if (!isRtbAd) {
      synchronized (INCENTIVIZED_ADS_LOCK) {
        final Bundle serverParameters = adConfiguration.getServerParameters();
        String sdkKey = AppLovinUtils.retrieveSdkKey(context, serverParameters);
        if (TextUtils.isEmpty(sdkKey)) {
          AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid SDK Key.", ERROR_DOMAIN);
          log(ERROR, error.getMessage());
          mediationAdLoadCallback.onFailure(error);
          return;
        }

        AppLovinInitializer.getInstance()
            .initialize(context, sdkKey, new OnInitializeSuccessListener() {
              @Override
              public void onInitializeSuccess(@NonNull String sdkKey) {
                mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);
                mSdk = AppLovinUtils.retrieveSdk(serverParameters, context);
                mNetworkExtras = adConfiguration.getMediationExtras();
                mMediationAdLoadCallback = mediationAdLoadCallback;

                String logMessage = String
                    .format("Requesting rewarded video for zone '%s'", mZoneId);
                log(DEBUG, logMessage);

                // Check if incentivized ad for zone already exists.
                if (INCENTIVIZED_ADS.containsKey(mZoneId)) {
                  mIncentivizedInterstitial = INCENTIVIZED_ADS.get(mZoneId);
                  AdError error = new AdError(ERROR_AD_ALREADY_REQUESTED,
                      "Cannot load multiple rewarded ads with the same Zone ID. "
                          + "Display one ad before attempting to load another.", ERROR_DOMAIN);
                  log(ERROR, error.getMessage());
                  mMediationAdLoadCallback.onFailure(error);
                } else {
                  // If this is a default Zone, create the incentivized ad normally
                  if (DEFAULT_ZONE.equals(mZoneId)) {
                    mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mSdk);
                  }
                  // Otherwise, use the Zones API
                  else {
                    mIncentivizedInterstitial = AppLovinIncentivizedInterstitial
                        .create(mZoneId, mSdk);
                  }
                  INCENTIVIZED_ADS.put(mZoneId, mIncentivizedInterstitial);
                }
              }
            });
      }

      mIncentivizedInterstitial.preload(this);
    } else {
      mMediationAdLoadCallback = mediationAdLoadCallback;
      mNetworkExtras = adConfiguration.getMediationExtras();
      mSdk = AppLovinUtils.retrieveSdk(adConfiguration.getServerParameters(), context);

      // Create rewarded video object
      mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mSdk);
      // Load ad!
      mSdk.getAdService().loadNextAdForAdToken(adConfiguration.getBidResponse(), this);
    }
  }

  @Override
  public void showAd(@NonNull Context context) {
    mSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(mNetworkExtras));
    final AppLovinIncentivizedAdListener listener =
        new AppLovinIncentivizedAdListener(adConfiguration, mRewardedAdCallback);

    if (!isRtbAd) {
      if (mZoneId != null) {
        String logMessage = String.format("Showing rewarded video for zone '%s'", mZoneId);
        log(DEBUG, logMessage);
      }
      if (!mIncentivizedInterstitial.isAdReadyToDisplay()) {
        AdError error = new AdError(ERROR_PRESENTATON_AD_NOT_READY, "Ad not ready to show.",
            ERROR_DOMAIN);
        log(ERROR, error.getMessage());
        mRewardedAdCallback.onAdFailedToShow(error);
      } else {
        mIncentivizedInterstitial.show(context, listener, listener, listener, listener);
      }
    } else {
      mIncentivizedInterstitial.show(ad, context, listener, listener, listener, listener);
    }
  }

  @Override
  public void collectSignals(RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    final MediationConfiguration config = rtbSignalData.getConfiguration();

    // Check if supported ad format
    if (config.getFormat() == AdFormat.NATIVE) {
      AdError error = new AdError(ERROR_AD_FORMAT_UNSUPPORTED,
          "Requested to collect signal for unsupported native ad format. Ignoring...",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      signalCallbacks.onFailure(error);
      return;
    }

    // Check if the publisher provided extra parameters
    if (rtbSignalData.getNetworkExtras() != null) {
      log(INFO, "Extras for signal collection: " + rtbSignalData.getNetworkExtras());
    }

    AppLovinSdk sdk =
        AppLovinUtils.retrieveSdk(config.getServerParameters(), rtbSignalData.getContext());
    String bidToken = sdk.getAdService().getBidToken();

    if (!TextUtils.isEmpty(bidToken)) {
      log(INFO, "Generated bid token: " + bidToken);
      signalCallbacks.onSuccess(bidToken);
    } else {
      AdError error = new AdError(ERROR_EMPTY_BID_TOKEN, "Failed to generate bid token.",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      signalCallbacks.onFailure(error);
    }
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {

    mRtbBannerRenderer =
        new AppLovinRtbBannerRenderer(mediationBannerAdConfiguration, mediationAdLoadCallback);
    mRtbBannerRenderer.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {

    mRtbInterstitialRenderer =
        new AppLovinRtbInterstitialRenderer(mediationInterstitialAdConfiguration, callback);
    mRtbInterstitialRenderer.loadAd();
  }

  @Override
  public void adReceived(final AppLovinAd appLovinAd) {
    ad = appLovinAd;
    Log.d("INFO", "Rewarded video did load ad: " + ad.getAdIdNumber());
    AppLovinSdkUtils.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            mRewardedAdCallback = mMediationAdLoadCallback.onSuccess(AppLovinMediationAdapter.this);
          }
        });
  }

  @Override
  public void failedToReceiveAd(final int code) {
    if (!isRtbAd) {
      INCENTIVIZED_ADS.remove(mZoneId);
    }
    AppLovinSdkUtils.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            AdError error = AppLovinUtils.getAdError(code);
            log(WARN, error.getMessage());
            mMediationAdLoadCallback.onFailure(error);
          }
        });
  }
}
