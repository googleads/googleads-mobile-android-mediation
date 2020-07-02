package com.google.ads.mediation.applovin;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;

public class AppLovinMediationAdapter extends RtbAdapter
    implements MediationRewardedAd, AppLovinAdLoadListener {

  private static final String TAG = AppLovinMediationAdapter.class.getSimpleName();
  private static final String DEFAULT_ZONE = "";
  private static boolean isRtbAd = true;

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

  /** Applovin adapter errors. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_BANNER_SIZE_MISMATCH,
        ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
        ERROR_NULL_CONTEXT,
        ERROR_EMPTY_BID_TOKEN,
        ERROR_AD_ALREADY_REQUESTED,
        ERROR_PRESENTATON_AD_NOT_READY,
        ERROR_MAPPING_NATIVE_ASSETS,
        ERROR_AD_FORMAT_UNSUPPORTED,
        ERROR_CONTEXT_NOT_ACTIVITY
      })
  public @interface AdapterError {}

  /** Banner size mismatch. */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 101;
  /** App did not request unified native ads. */
  public static final int ERROR_REQUIRES_UNIFIED_NATIVE_ADS = 102;
  /** Context is null. */
  public static final int ERROR_NULL_CONTEXT = 103;
  /** AppLovin bid token is empty. */
  public static final int ERROR_EMPTY_BID_TOKEN = 104;
  /** Requested multiple ads for the same zone. AppLovin can only load 1 ad at a time per zone. */
  public static final int ERROR_AD_ALREADY_REQUESTED = 105;
  /** Ad is not ready to display. */
  public static final int ERROR_PRESENTATON_AD_NOT_READY = 106;
  /** Native ad is missing required assets. */
  public static final int ERROR_MAPPING_NATIVE_ASSETS = 107;
  /** Adapter does not support the ad format being requested. */
  public static final int ERROR_AD_FORMAT_UNSUPPORTED = 108;
  /** Context is not an Activity instance. */
  public static final int ERROR_CONTEXT_NOT_ACTIVITY = 109;

  /** Creates a formatted adapter error string given a code and description. */
  public static String createAdapterError(@AdapterError int code, @NonNull String description) {
    return String.format("%d: %s", code, description);
  }

  public static String createSDKError(int code) {
    String message = "AppLovin SDK returned a failure callback.";
    return String.format("%d: %s", code, message);
  }

  @Override
  public void initialize(
      Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {
    log(DEBUG, "Attempting to initialize SDK.");

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed(
          "AppLovin requires an Activity context to initialize.");
      return;
    }
    Activity activity = (Activity) context;

    if (AppLovinUtils.androidManifestHasValidSdkKey(activity)) {
      AppLovinSdk.getInstance(activity).initializeSdk();
    }

    for (MediationConfiguration mediationConfig : mediationConfigurations) {
      AppLovinSdk sdk = AppLovinUtils.retrieveSdk(mediationConfig.getServerParameters(), activity);
      sdk.initializeSdk();
    }
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.VERSION_NAME;
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
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
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
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    adConfiguration = mediationRewardedAdConfiguration;
    Context context = adConfiguration.getContext();

    if (!(context instanceof Activity)) {
      String adapterError =
          createAdapterError(
              ERROR_CONTEXT_NOT_ACTIVITY, "AppLovin requires an Activity context to load ads.");
      log(ERROR, "Failed to load rewarded ad from AppLovin: " + adapterError);
      mediationAdLoadCallback.onFailure(adapterError);
      return;
    }

    if (mediationRewardedAdConfiguration.getBidResponse().equals("")) {
      isRtbAd = false;
    }

    if (!isRtbAd) {
      synchronized (INCENTIVIZED_ADS_LOCK) {
        Bundle serverParameters = adConfiguration.getServerParameters();
        mZoneId = AppLovinUtils.retrieveZoneId(serverParameters);
        mSdk = AppLovinUtils.retrieveSdk(serverParameters, context);
        mNetworkExtras = adConfiguration.getMediationExtras();
        mMediationAdLoadCallback = mediationAdLoadCallback;

        String logMessage = String.format("Requesting rewarded video for zone '%s'", mZoneId);
        log(DEBUG, logMessage);

        // Check if incentivized ad for zone already exists.
        if (INCENTIVIZED_ADS.containsKey(mZoneId)) {
          mIncentivizedInterstitial = INCENTIVIZED_ADS.get(mZoneId);
          String errorMessage =
              createAdapterError(
                  ERROR_AD_ALREADY_REQUESTED,
                  "Cannot load multiple rewarded ads with the same Zone ID. "
                      + "Display one ad before attempting to load another.");
          log(ERROR, errorMessage);
          mMediationAdLoadCallback.onFailure(errorMessage);
        } else {
          // If this is a default Zone, create the incentivized ad normally
          if (DEFAULT_ZONE.equals(mZoneId)) {
            mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mSdk);
          }
          // Otherwise, use the Zones API
          else {
            mIncentivizedInterstitial = AppLovinIncentivizedInterstitial.create(mZoneId, mSdk);
          }
          INCENTIVIZED_ADS.put(mZoneId, mIncentivizedInterstitial);
        }
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
  public void showAd(Context context) {
    mSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(mNetworkExtras));
    String logMessage = String.format("Showing rewarded video for zone '%s'", mZoneId);
    log(DEBUG, logMessage);
    final AppLovinIncentivizedAdListener listener =
        new AppLovinIncentivizedAdListener(adConfiguration, mRewardedAdCallback);

    if (!isRtbAd) {
      if (!mIncentivizedInterstitial.isAdReadyToDisplay()) {
        String errorMessage =
            createAdapterError(ERROR_PRESENTATON_AD_NOT_READY, "Ad Failed to show.");
        mRewardedAdCallback.onAdFailedToShow(errorMessage);
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
      String errorMessage =
          createAdapterError(
              ERROR_AD_FORMAT_UNSUPPORTED,
              "Requested to collect signal for unsupported native ad format. Ignoring...");
      handleCollectSignalsFailure(errorMessage, signalCallbacks);
      return;
    }

    // Check if the publisher provided extra parameters
    if (rtbSignalData.getNetworkExtras() != null) {
      Log.i(TAG, "Extras for signal collection: " + rtbSignalData.getNetworkExtras());
    }

    AppLovinSdk sdk =
        AppLovinUtils.retrieveSdk(config.getServerParameters(), rtbSignalData.getContext());
    String bidToken = sdk.getAdService().getBidToken();

    if (!TextUtils.isEmpty(bidToken)) {
      Log.i(TAG, "Generated bid token: " + bidToken);
      signalCallbacks.onSuccess(bidToken);
    } else {
      String errorMessage =
          createAdapterError(ERROR_EMPTY_BID_TOKEN, "Failed to generate bid token.");
      handleCollectSignalsFailure(errorMessage, signalCallbacks);
    }
  }

  private void handleCollectSignalsFailure(String errorMessage, SignalCallbacks signalCallbacks) {
    Log.e(TAG, errorMessage);
    signalCallbacks.onFailure(errorMessage);
  }

  @Override
  public void loadBannerAd(
      MediationBannerAdConfiguration mediationBannerAdConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {

    mRtbBannerRenderer =
        new AppLovinRtbBannerRenderer(mediationBannerAdConfiguration, mediationAdLoadCallback);
    mRtbBannerRenderer.loadAd();
  }

  @Override
  public void loadInterstitialAd(
      MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {

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
    final String errorMessage = createSDKError(code);
    log(ERROR, errorMessage);
    if (!isRtbAd) {
      INCENTIVIZED_ADS.remove(mZoneId);
    }
    AppLovinSdkUtils.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            mMediationAdLoadCallback.onFailure(errorMessage);
          }
        });
  }
}
