package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.rtb.VungleRtbBannerAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd;
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
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Mediation network adapter for Vungle.
 */
public class VungleMediationAdapter extends RtbAdapter
    implements MediationRewardedAd, LoadAdCallback, PlayAdCallback {

  private static final String TAG = VungleMediationAdapter.class.getSimpleName();
  public static final String KEY_APP_ID = "appid";

  private AdConfig mAdConfig;
  private String mUserID;
  private String mPlacement;
  private String mAdMarkup;
  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private static final HashMap<String, WeakReference<VungleMediationAdapter>> mPlacementsInUse =
      new HashMap<>();

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  /**
   * Vungle adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.vungle";

  /**
   * Vungle SDK error domain.
   */
  public static final String VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.warren";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          ERROR_AD_ALREADY_LOADED,
          ERROR_VUNGLE_BANNER_NULL,
          ERROR_INITIALIZATION_FAILURE,
          ERROR_CANNOT_PLAY_AD,
      })

  public @interface AdapterError {

  }

  /**
   * Server parameters, such as app ID or placement ID, are invalid.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Vungle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Vungle requires an {@link Activity} context to request ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * Vungle SDK cannot load multiple ads for the same placement ID.
   */
  public static final int ERROR_AD_ALREADY_LOADED = 104;

  /**
   * Vungle SDK failed to initialize.
   */
  public static final int ERROR_INITIALIZATION_FAILURE = 105;

  /**
   * Vungle SDk returned a successful load callback, but Banners.getBanner() or Vungle.getNativeAd()
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
  public void collectSignals(@NonNull RtbSignalData rtbSignalData,
      @NonNull SignalCallbacks signalCallbacks) {
    String token = Vungle.getAvailableBidTokens(rtbSignalData.getContext());
    Log.d(TAG, "token=" + token);
    signalCallbacks.onSuccess(token);
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
      if (initializationCompleteCallback != null) {
        AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.",
            ERROR_DOMAIN);
        Log.w(TAG, error.getMessage());
        initializationCompleteCallback.onInitializationFailed(error.getMessage());
      }
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
                Log.w(TAG, error.getMessage());
                initializationCompleteCallback.onInitializationFailed(error.getMessage());
              }

            });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    mMediationAdLoadCallback = mediationAdLoadCallback;

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
          "Only a maximum of one ad can be loaded per placement.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mediationRewardedAdConfiguration.getBidResponse();
    Log.d(TAG, "Render rewarded mAdMarkup=" + mAdMarkup);

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);

    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());

    VungleInitializer.getInstance()
        .initialize(
            appID,
            mediationRewardedAdConfiguration.getContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(mUserID, null, null, null, null);
                mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));

                if (Vungle.canPlayAd(mPlacement, mAdMarkup)) {
                  mMediationRewardedAdCallback =
                      mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
                  return;
                }

                Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, VungleMediationAdapter.this);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.getMessage());
                mMediationAdLoadCallback.onFailure(error);
                mPlacementsInUse.remove(mPlacement);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    Vungle.playAd(mPlacement, mAdMarkup, mAdConfig, VungleMediationAdapter.this);
  }

  /**
   * {@link LoadAdCallback} implementation from Vungle.
   */
  @Override
  public void onAdLoad(final String placementId) {
    mHandler.post(
        new Runnable() {
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

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  /**
   * {@link PlayAdCallback} implementation from Vungle
   */
  @Override
  public void onAdStart(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdOpened();
            }
          }
        });
  }

  @Override
  @Deprecated
  public void onAdEnd(final String placementId, final boolean wasSuccessfulView,
      final boolean wasCallToActionClicked) {
  }

  @Override
  public void onAdEnd(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdClosed();
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdClick(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.reportAdClicked();
            }
          }
        });
  }

  @Override
  public void onAdRewarded(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onVideoComplete();
              mMediationRewardedAdCallback.onUserEarnedReward(new VungleReward("vungle", 1));
            }
          }
        });
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    // no op
  }

  // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
  // ad request to Vungle fails, and when an ad fails to play.
  @Override
  public void onError(final String placementId, final VungleException throwable) {
    final AdError error = getAdError(throwable);
    Log.w(TAG, error.getMessage());
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(error);
            }
            if (mMediationRewardedAdCallback != null) {
              AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                  "Failed to show ad from Vungle.", ERROR_DOMAIN);
              mMediationRewardedAdCallback.onAdFailedToShow(error);
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdViewed(String placementId) {
    mMediationRewardedAdCallback.onVideoStart();
    mMediationRewardedAdCallback.reportAdImpression();
  }

  /**
   * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
   */
  private static class VungleReward implements RewardItem {

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

    @NonNull
    @Override
    public String getType() {
      return mType;
    }
  }

  @Override
  public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbRewardedAd()...");
      VungleInitializer.getInstance()
              .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());
      loadRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void loadRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbInterstitialAd()...");
    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment());
    VungleRtbInterstitialAd rtbInterstitialAd = new VungleRtbInterstitialAd(
        mediationInterstitialAdConfiguration, mediationAdLoadCallback);
    rtbInterstitialAd.render();
  }

  @Override
  public void loadRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    Log.d(TAG, "loadRtbBannerAd()...");
    VungleInitializer.getInstance()
            .updateCoppaStatus(mediationBannerAdConfiguration.taggedForChildDirectedTreatment());
    VungleRtbBannerAd rtbBannerAd = new VungleRtbBannerAd(mediationBannerAdConfiguration,
        mediationAdLoadCallback);
    rtbBannerAd.render();
  }
}
