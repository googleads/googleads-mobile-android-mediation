package com.google.ads.mediation.mopub;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.dfp.adapters.BuildConfig;
import com.mopub.mobileads.dfp.adapters.MoPubAdapter.BundleBuilder;
import com.mopub.mobileads.dfp.adapters.MoPubAdapterRewardedListener;
import com.mopub.nativeads.NativeErrorCode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;

/**
 * A {@link com.google.ads.mediation.mopub.MoPubMediationAdapter} used to mediate rewarded video ads
 * from MoPub.
 */
public class MoPubMediationAdapter extends Adapter
    implements MediationRewardedAd, MoPubAdapterRewardedListener {

  static final String TAG = MoPubMediationAdapter.class.getSimpleName();
  private static final String MOPUB_AD_UNIT_KEY = "adUnitId";

  private String adUnitID = "";

  @Nullable
  private String customRewardData = null;

  // TODO: Remove `adExpired` parameter once MoPub fixes MoPubRewardedVideos.hasRewardedVideo()
  //  to return false for expired ads.
  private boolean adExpired;

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  // region Error codes.
  // MoPub adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mopub";

  // MoPub SDK error domain.
  public static final String MOPUB_SDK_ERROR_DOMAIN = "com.mopub";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          // ERROR_MOPUB_INITIALIZATION,
          ERROR_WRONG_NATIVE_TYPE,
          // ERROR_FAILED_PLAYBACK,
          ERROR_AD_ALREADY_LOADED,
          ERROR_AD_EXPIRED,
          // ERROR_HAS_REWARDED_AD,
          ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
          ERROR_DOWNLOADING_NATIVE_ASSETS,
          ERROR_AD_NOT_READY,
          ERROR_MINIMUM_BANNER_SIZE
      })
  public @interface AdapterError {

  }

  /**
   * Missing or invalid server parameters (e.g. ad unit ID is null).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a MoPub supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * MoPub requires an {@link Activity} context to request ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

  /**
   * MoPub SDK failed to initialize. This error code is never used.
   */
  // public static final int ERROR_MOPUB_INITIALIZATION = 104;

  /**
   * Loaded native ad is not a {@link com.mopub.nativeads.StaticNativeAd }instance.
   */
  public static final int ERROR_WRONG_NATIVE_TYPE = 105;

  /**
   * This error code is never used.
   */
  // public static final int ERROR_FAILED_PLAYBACK = 106;

  /**
   * MoPub can only load one rewarded ad at a time.
   */
  public static final int ERROR_AD_ALREADY_LOADED = 107;

  /**
   * MoPub's rewarded video ad has expired.
   */
  public static final int ERROR_AD_EXPIRED = 108;

  /**
   * This error code is never used.
   */
  // public static final int ERROR_HAS_REWARDED_AD = 109;

  /**
   * A non-Unified native ad was requested.
   */
  public static final int ERROR_REQUIRES_UNIFIED_NATIVE_ADS = 110;

  /**
   * Failed to download native ad assets.
   */
  public static final int ERROR_DOWNLOADING_NATIVE_ASSETS = 111;

  /**
   * MoPub does not have a rewarded ad ready to show.
   */
  public static final int ERROR_AD_NOT_READY = 112;

  /**
   * The loaded ad was smaller than the minimum required banner size.
   */
  public static final int ERROR_MINIMUM_BANNER_SIZE = 113;

  public static int getMediationErrorCode(@NonNull MoPubErrorCode moPubErrorCode) {
    switch (moPubErrorCode) {
      case AD_SUCCESS:
        return 0;
      case DO_NOT_TRACK:
        return 1;
      case UNSPECIFIED:
        return 2;
      case NO_FILL:
        return 3;
      case WARMUP:
        return 4;
      case SERVER_ERROR:
        return 5;
      case INTERNAL_ERROR:
        return 6;
      case RENDER_PROCESS_GONE_WITH_CRASH:
        return 7;
      case RENDER_PROCESS_GONE_UNSPECIFIED:
        return 8;
      case CANCELLED:
        return 9;
      case MISSING_AD_UNIT_ID:
        return 10;
      case NO_CONNECTION:
        return 11;
      case ADAPTER_NOT_FOUND:
        return 12;
      case ADAPTER_CONFIGURATION_ERROR:
        return 13;
      case ADAPTER_INITIALIZATION_SUCCESS:
        return 14;
      case EXPIRED:
        return 15;
      case NETWORK_TIMEOUT:
        return 16;
      case NETWORK_NO_FILL:
        return 17;
      case NETWORK_INVALID_STATE:
        return 18;
      case MRAID_LOAD_ERROR:
        return 19;
      case VIDEO_CACHE_ERROR:
        return 20;
      case VIDEO_DOWNLOAD_ERROR:
        return 21;
      case GDPR_DOES_NOT_APPLY:
        return 22;
      case REWARDED_CURRENCIES_PARSING_ERROR:
        return 23;
      case REWARD_NOT_SELECTED:
        return 24;
      case VIDEO_NOT_AVAILABLE:
        return 25;
      case VIDEO_PLAYBACK_ERROR:
        return 26;
      case TOO_MANY_REQUESTS:
        return 27;
      case HTML_LOAD_ERROR:
        return 28;
      case INLINE_LOAD_ERROR:
        return 29;
      case FULLSCREEN_LOAD_ERROR:
        return 30;
      case INLINE_SHOW_ERROR:
        return 31;
      case FULLSCREEN_SHOW_ERROR:
        return 32;
      case AD_NOT_AVAILABLE:
        return 33;
      case AD_SHOW_ERROR:
        return 34;
    }
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    return 99;
  }

  public static int getMediationErrorCode(@NonNull NativeErrorCode nativeErrorCode) {
    switch (nativeErrorCode) {
      case AD_SUCCESS:
        return 1000;
      case EMPTY_AD_RESPONSE:
        return 1001;
      case INVALID_RESPONSE:
        return 1002;
      case IMAGE_DOWNLOAD_FAILURE:
        return 1003;
      case INVALID_REQUEST_URL:
        return 1004;
      case UNEXPECTED_RESPONSE_CODE:
        return 1005;
      case SERVER_ERROR_RESPONSE_CODE:
        return 1006;
      case CONNECTION_ERROR:
        return 1007;
      case UNSPECIFIED:
        return 1008;
      case NETWORK_INVALID_REQUEST:
        return 1009;
      case NETWORK_TIMEOUT:
        return 1010;
      case NETWORK_NO_FILL:
        return 1011;
      case NETWORK_INVALID_STATE:
        return 1012;
      case NATIVE_RENDERER_CONFIGURATION_ERROR:
        return 1013;
      case NATIVE_ADAPTER_CONFIGURATION_ERROR:
        return 1014;
      case NATIVE_ADAPTER_NOT_FOUND:
        return 1015;
      case TOO_MANY_REQUESTS:
        return 1016;
    }
    // Error '1099' to indicate that the error is new and has not been supported by the adapter yet.
    return 1099;
  }
  // endregion

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
    String versionString = MoPub.SDK_VERSION;
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
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      AdError initializationError = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "MoPub SDK requires an Activity context to initialize.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();

      // The MoPub SDK requires any valid Ad Unit ID in order to initialize their SDK.
      adUnitID = serverParameters.getString(MOPUB_AD_UNIT_KEY);
      if (!TextUtils.isEmpty(adUnitID)) {
        break;
      }
    }

    if (TextUtils.isEmpty(adUnitID)) {
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid MoPub Ad Unit ID.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnitID).build();
    MoPubSingleton.getInstance()
        .initializeMoPubSDK(context, sdkConfiguration, new SdkInitializationListener() {
          @Override
          public void onInitializationFinished() {
            initializationCompleteCallback.onInitializationSucceeded();
          }
        });
  }

  @Override
  public void loadRewardedAd(
      @NonNull final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    adUnitID = serverParameters.getString(MOPUB_AD_UNIT_KEY);

    if (TextUtils.isEmpty(adUnitID)) {
      AdError loadError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid MoPub Ad Unit ID.", ERROR_DOMAIN);
      Log.e(TAG, loadError.toString());
      mediationAdLoadCallback.onFailure(loadError);
      return;
    }

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    customRewardData = mediationExtras.getString(BundleBuilder.ARG_CUSTOM_REWARD_DATA);

    mAdLoadCallback = mediationAdLoadCallback;
    MoPubRewardedVideoManager.RequestParameters requestParameters =
        new MoPubRewardedVideoManager.RequestParameters(
            MoPubSingleton.getKeywords(mediationRewardedAdConfiguration, false),
            MoPubSingleton.getKeywords(mediationRewardedAdConfiguration, true),
            mediationRewardedAdConfiguration.getLocation());
    MoPubSingleton.getInstance()
        .loadRewardedAd(context, adUnitID, requestParameters, MoPubMediationAdapter.this);
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (adExpired && mRewardedAdCallback != null) {
      AdError showError = new AdError(ERROR_AD_EXPIRED, "Failed to show a MoPub rewarded video. "
          + "The MoPub Ad has expired. Please make a new Ad Request.", ERROR_DOMAIN);
      Log.e(TAG, showError.toString());
      mRewardedAdCallback.onAdFailedToShow(showError);
    } else if (!adExpired) {
      boolean didShow = MoPubSingleton.getInstance().showRewardedAd(adUnitID, customRewardData);
      if (!didShow && mRewardedAdCallback != null) {
        String errorMessage = String
            .format("MoPub does not have a rewarded ad ready to show for ad unit ID: %s", adUnitID);
        AdError showError = new AdError(ERROR_AD_NOT_READY, errorMessage, ERROR_DOMAIN);
        Log.i(TAG, showError.toString());
        mRewardedAdCallback.onAdFailedToShow(showError);
      }
    }
  }


  /**
   * {@link MoPubRewardedVideoListener} implementation
   */
  @Override
  public void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.toString());
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(loadError);
    }
  }

  @Override
  public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(MoPubMediationAdapter.this);
    }
  }

  @Override
  public void onRewardedVideoLoadFailure(@NonNull String adUnitId,
      @NonNull MoPubErrorCode errorCode) {
    AdError loadError = new AdError(getMediationErrorCode(errorCode), errorCode.toString(),
        ERROR_DOMAIN);
    Log.i(TAG, loadError.toString());

    // MoPub Rewarded video ads expire after 4 hours.
    if (errorCode == MoPubErrorCode.EXPIRED) {
      MoPubSingleton.getInstance().adExpired(adUnitId, MoPubMediationAdapter.this);
      adExpired = true;
    }

    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(loadError);
    }
  }

  @Override
  public void onRewardedVideoStarted(@NonNull String adUnitId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      mRewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void onRewardedVideoPlaybackError(@NonNull String adUnitId,
      @NonNull MoPubErrorCode errorCode) {
    AdError playbackError = new AdError(getMediationErrorCode(errorCode), errorCode.toString(),
        ERROR_DOMAIN);
    Log.i(TAG, "Failed to playback MoPub rewarded video: " + playbackError.toString());

    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow(playbackError);
    }
  }

  @Override
  public void onRewardedVideoClicked(@NonNull String adUnitId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds,
      @NonNull final MoPubReward reward) {
    Preconditions.checkNotNull(reward);

    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoComplete();
      mRewardedAdCallback.onUserEarnedReward(
          new RewardItem() {
            @NonNull
            @Override
            public String getType() {
              return reward.getLabel();
            }

            @Override
            public int getAmount() {
              return reward.getAmount();
            }
          });
    }
  }

  @Override
  public void onRewardedVideoClosed(@NonNull String adUnitId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
  }
}
