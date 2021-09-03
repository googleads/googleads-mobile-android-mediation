package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
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
import com.google.android.gms.ads.mediation.VersionInfo;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A {@link Adapter} used to load and show Vungle interstitial/banner ads using Google Mobile Ads
 * SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter extends Adapter
    implements MediationInterstitialAd, MediationBannerAd {

  private static final String TAG = VungleInterstitialAdapter.class.getSimpleName();

  private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback;
  private MediationInterstitialAdCallback mediationInterstitialAdCallback;
  private VungleManager mVungleManager;
  private AdConfig mAdConfig;
  private String mPlacementForPlay;
  private String mAdMarkup;

  // banner/MREC
  private VungleBannerAdapter vungleBannerAdapter;

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
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  @NonNull
  public VersionInfo getSDKVersionInfo() {
    String versionString = com.vungle.warren.BuildConfig.VERSION_NAME;
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(
      @NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (Vungle.isInitialized()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    HashSet<String> appIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appIDFromServer = serverParameters.getString(KEY_APP_ID);

      if (!TextUtils.isEmpty(appIDFromServer)) {
        appIDs.add(appIDFromServer);
      }
    }

    int count = appIDs.size();
    if (count <= 0) {
      initializationCompleteCallback.onInitializationFailed(
          "Initialization failed: Missing or Invalid App ID.");
      return;
    }

    String appID = appIDs.iterator().next();
    if (count > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.",
              KEY_APP_ID, appIDs.toString(), appID);
      Log.w(TAG, logMessage);
    }

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context.getApplicationContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
              }

              @Override
              public void onInitializeError(AdError error) {
                initializationCompleteCallback.onInitializationFailed(
                    "Initialization Failed: " + error.getMessage());
              }
            });
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    mMediationAdLoadCallback = mediationAdLoadCallback;

    Log.d(TAG,"loadInterstitialAd()....");

    Bundle mediationExtras = mediationInterstitialAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mVungleManager = VungleManager.getInstance();
    mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mMediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mediationInterstitialAdConfiguration.getBidResponse();
    Log.d(TAG, "Render interstitial mAdMarkup=" + mAdMarkup);

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            mediationInterstitialAdConfiguration.getContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                loadAd();
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                if (mMediationAdLoadCallback != null) {
                  mMediationAdLoadCallback.onFailure(error);
                }
              }
            });
  }

  private void loadAd() {
    if (Vungle.canPlayAd(mPlacementForPlay, mAdMarkup)) {
      if (mMediationAdLoadCallback != null) {
        mediationInterstitialAdCallback = mMediationAdLoadCallback
            .onSuccess(VungleInterstitialAdapter.this);
      }
      return;
    }

    // Placement ID is not what Vungle's SDK gets back after init/config.
    if (!mVungleManager.isValidPlacement(mPlacementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    Vungle.loadAd(mPlacementForPlay, mAdMarkup, mAdConfig, new LoadAdCallback() {
      @Override
      public void onAdLoad(String placementID) {
        if (mMediationAdLoadCallback != null) {
          mediationInterstitialAdCallback = mMediationAdLoadCallback
              .onSuccess(VungleInterstitialAdapter.this);
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w("TAG", error.getMessage());
        if (mMediationAdLoadCallback != null) {
          mMediationAdLoadCallback.onFailure(error);
        }
      }
    });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacementForPlay, mAdMarkup, mAdConfig, new PlayAdCallback() {

      @Override
      public void creativeId(String creativeId) {
        // no-op
      }

      @Override
      public void onAdStart(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdOpened();
        }
      }

      @Override
      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
        // Deprecated, no-op.
      }

      @Override
      public void onAdEnd(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdClick(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdClicked();
        }
      }

      @Override
      public void onAdRewarded(String placementID) {
        // No-op for interstitial ads.
      }

      @Override
      public void onAdLeftApplication(String placementID) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdLeftApplication();
        }
      }

      @Override
      public void onError(String placementID, VungleException exception) {
        AdError error = VungleMediationAdapter.getAdError(exception);
        Log.w("TAG", error.getMessage());
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.onAdClosed();
        }
      }

      @Override
      public void onAdViewed(String id) {
        if (mediationInterstitialAdCallback != null) {
          mediationInterstitialAdCallback.reportAdImpression();
        }
      }
    });
  }

  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    Log.d(TAG,"loadBannerAd()....");

    Bundle mediationExtras = mediationBannerAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);

    mVungleManager = VungleManager.getInstance();

    String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placementForPlay + " ### Adapter instance: " + this
            .hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationBannerAdConfiguration.getContext();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    String uniqueRequestId = config.getRequestUniqueId();
    if (!mVungleManager.canRequestBannerAd(placementForPlay, uniqueRequestId)) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Vungle adapter does not support multiple banner instances for same placement.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    vungleBannerAdapter = new VungleBannerAdapter(placementForPlay, uniqueRequestId, adConfig,
        VungleInterstitialAdapter.this);
    Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

    VungleBannerAd vungleBanner = new VungleBannerAd(placementForPlay, vungleBannerAdapter);
    mVungleManager.registerBannerAd(placementForPlay, vungleBanner);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    vungleBannerAdapter
        .requestBannerAd(config.getAppId(), mediationBannerAdConfiguration,
            mediationAdLoadCallback);
  }

  @NonNull
  @Override
  public View getView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return vungleBannerAdapter.getAdLayout();
  }

  private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(
        TAG,
        "Found closest ad size: " + closestSize.toString() + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(BANNER_SHORT);
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      adConfig.setAdSize(BANNER);
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(VUNGLE_MREC);
    }

    return true;
  }
}