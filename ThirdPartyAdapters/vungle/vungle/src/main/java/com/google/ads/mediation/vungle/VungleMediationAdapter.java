package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbNativeAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
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
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.PlacementFinder;
import java.util.HashSet;
import java.util.List;

public class VungleMediationAdapter extends RtbAdapter implements MediationRewardedAd,
    RewardedAdListener {

  public static final String TAG = VungleMediationAdapter.class.getSimpleName();

  public static final String KEY_APP_ID = "appid";
  public static final String KEY_USER_ID = "userId";
  public static final String KEY_ORIENTATION = "adOrientation";
  public static final String KEY_PLAY_PLACEMENT = "playPlacement";

  private VungleRtbInterstitialAd rtbInterstitialAd;
  private VungleRtbRewardedAd rtbRewardedAd;
  private VungleRtbRewardedAd rtbRewardedInterstitialAd;
  private VungleRtbBannerAd rtbBannerAd;
  private VungleRtbNativeAd rtbNativeAd;

  private String placement;
  private RewardedAd rewardedAd;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  /**
   * Vungle adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.vungle";

  /**
   * Vungle SDK error domain.
   */
  public static final String VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.warren";

  /**
   * Server parameters, such as app ID or placement ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Vungle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Vungle requires an {@link android.app.Activity} context to request ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * Vungle SDK failed to initialize.
   */
  public static final int ERROR_INITIALIZATION_FAILURE = 105;

  /**
   * Vungle SDK returned a successful load callback, but Banners.getBanner() or Vungle.getNativeAd()
   * returned null.
   */
  public static final int ERROR_VUNGLE_BANNER_NULL = 106;

  /**
   * Vungle SDK is not ready to play the ad.
   */
  public static final int ERROR_CANNOT_PLAY_AD = 107;

  /**
   * Convert the given Vungle exception into the appropriate custom error code.
   */
  @NonNull
  public static AdError getAdError(@NonNull VungleException exception) {
    return new AdError(exception.getExceptionCode(), exception.getLocalizedMessage(),
        VUNGLE_SDK_ERROR_DOMAIN);
  }

  @NonNull
  @Override
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

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = com.vungle.ads.BuildConfig.VERSION_NAME;
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
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String token = VungleAds.getBiddingToken(rtbSignalData.getContext());
    token = token == null ? "" : token;
    Log.d(TAG, "token=" + token);
    signalCallbacks.onSuccess(token);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (VungleAds.isInitialized()) {
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
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    } else if (count > 1) {
      String message = String.format("Multiple '%s' entries found: %s.", KEY_APP_ID, appIDs);
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, message, ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }

    String appID = appIDs.iterator().next();
    VungleInitializer.getInstance()
        .initialize(
            appID, context,
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                initializationCompleteCallback.onInitializationSucceeded();
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                initializationCompleteCallback.onInitializationFailed(error.toString());
              }

            });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    if (placement == null || placement.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    AdConfig adConfig = new AdConfig();
    if (mediationExtras.containsKey(KEY_ORIENTATION)) {
      adConfig.setAdOrientation(mediationExtras.getInt(KEY_ORIENTATION, AdConfig.AUTO_ROTATE));
    }

    String userId = mediationExtras.getString(KEY_USER_ID);

    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());

    Context context = mediationRewardedAdConfiguration.getContext();

    VungleInitializer.getInstance()
        .initialize(appID, context,
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                rewardedAd = new RewardedAd(context, placement, adConfig);
                rewardedAd.setAdListener(VungleMediationAdapter.this);
                if (!TextUtils.isEmpty(userId)) {
                  rewardedAd.setUserId(userId);
                }

                rewardedAd.load(null);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                VungleMediationAdapter.this.mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    rewardedAd.play();
  }

  @Override
  public void onAdClicked(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdEnd(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdImpression(@NonNull BaseAd baseAd) {
    mediationRewardedAdCallback.onVideoStart();
    mediationRewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onAdLoaded(@NonNull BaseAd baseAd) {
    if (mediationAdLoadCallback != null) {
      mediationRewardedAdCallback =
          mediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
    }
  }

  @Override
  public void onAdStart(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = getAdError(e);
    Log.w(TAG, error.toString());
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = getAdError(e);
    Log.w(TAG, error.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    // no-op
  }

  @Override
  public void onAdRewarded(@NonNull BaseAd baseAd) {
    if (mediationRewardedAdCallback != null) {
      mediationRewardedAdCallback.onVideoComplete();
      mediationRewardedAdCallback.onUserEarnedReward(new VungleReward("vungle", 1));
    }
  }

  /**
   * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
   */
  public static class VungleReward implements RewardItem {

    private final String type;
    private final int amount;

    public VungleReward(String type, int amount) {
      this.type = type;
      this.amount = amount;
    }

    @Override
    public int getAmount() {
      return amount;
    }

    @NonNull
    @Override
    public String getType() {
      return type;
    }
  }

  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    Log.d(TAG, "loadNativeAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationNativeAdConfiguration.taggedForChildDirectedTreatment());
    // Vungle waterfall and bidding Native ads use the same API.
    rtbNativeAd = new VungleRtbNativeAd(mediationNativeAdConfiguration,
        callback);
    rtbNativeAd.render();
  }

  @Override
  public void loadRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    Log.d(TAG, "loadRewardedInterstitialAd()...");
    Log.d(TAG, "Vungle adapter was asked to load a rewarded interstitial ad. "
        + "Using the rewarded ad request flow to load the ad to attempt to load a "
        + "rewarded interstitial ad from Vungle.");
    // Vungle Rewarded Interstitial ads use the same Rewarded Video API.
    loadRewardedAd(mediationRewardedAdConfiguration, callback);
  }

  public void loadRtbRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbRewardedAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());
    rtbRewardedAd = new VungleRtbRewardedAd(
        mediationRewardedAdConfiguration, mediationAdLoadCallback);
    rtbRewardedAd.render();
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbBannerAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationBannerAdConfiguration.taggedForChildDirectedTreatment());
    rtbBannerAd = new VungleRtbBannerAd(mediationBannerAdConfiguration,
        mediationAdLoadCallback);
    rtbBannerAd.render();
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbInterstitialAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment());
    rtbInterstitialAd = new VungleRtbInterstitialAd(
        mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    rtbInterstitialAd.render();
  }

  @Override
  public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    Log.d(TAG, "loadRtbNativeAd()...");
    VungleInitializer.getInstance()
        .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
    rtbNativeAd = new VungleRtbNativeAd(adConfiguration, callback);
    rtbNativeAd.render();
  }

  @Override
  public void loadRtbRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    Log.d(TAG, "loadRtbRewardedInterstitialAd()...");
    Log.d(TAG, "Vungle adapter was asked to load a rewarded interstitial ad. "
        + "Using the rewarded ad request flow to load the ad to attempt to load a "
        + "rewarded interstitial ad from Vungle.");
    VungleInitializer.getInstance()
        .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
    // Vungle Rewarded Interstitial ads use the same Rewarded Video API.
    rtbRewardedInterstitialAd = new VungleRtbRewardedAd(adConfiguration, callback);
    rtbRewardedInterstitialAd.render();
  }

}
