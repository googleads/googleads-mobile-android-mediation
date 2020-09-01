package com.google.ads.mediation.mopub;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

  @Nullable private String customRewardData = null;

  // TODO: Remove `adExpired` parameter once MoPub fixes MoPubRewardedVideos.hasRewardedVideo()
  //  to return false for expired ads.
  private boolean adExpired;

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  /** MoPub Network adapter errors. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_BANNER_SIZE_MISMATCH,
        ERROR_REQUIRES_ACTIVITY_CONTEXT,
        ERROR_MOPUB_INITIALIZATION,
        ERROR_WRONG_NATIVE_TYPE,
        ERROR_FAILED_PLAYBACK,
        ERROR_AD_ALREADY_LOADED,
        ERROR_AD_EXPIRED,
        ERROR_HAS_REWARDED_AD,
        ERROR_REQUIRES_UNIFIED_NATIVE_ADS,
        ERROR_DOWNLOADING_NATIVE_ASSETS,
        ERROR_AD_NOT_READY,
        ERROR_MINIMUM_BANNER_SIZE
      })
  public @interface AdapterError {}

  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;
  public static final int ERROR_MOPUB_INITIALIZATION = 104;
  public static final int ERROR_WRONG_NATIVE_TYPE = 105;
  public static final int ERROR_FAILED_PLAYBACK = 106;
  public static final int ERROR_AD_ALREADY_LOADED = 107;
  public static final int ERROR_AD_EXPIRED = 108;
  public static final int ERROR_HAS_REWARDED_AD = 109;
  public static final int ERROR_REQUIRES_UNIFIED_NATIVE_ADS = 110;
  public static final int ERROR_DOWNLOADING_NATIVE_ASSETS = 111;
  public static final int ERROR_AD_NOT_READY = 112;
  public static final int ERROR_MINIMUM_BANNER_SIZE = 113;

  /** Creates a formatted adapter error string given a code and description. */
  public static String createAdapterError(@AdapterError int code, String description) {
    return String.format("%d: %s", code, description);
  }

  public static String createSDKError(@NonNull MoPubErrorCode error) {
    return String.format("%d: %s", getMediationErrorCode(error), error.toString());
  }

  public static String createSDKError(@NonNull NativeErrorCode error) {
    return String.format("%d: %s", getMediationErrorCode(error), error.toString());
  }

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
    }
    // Error '1099' to indicate that the error is new and has not been supported by the adapter yet.
    return 1099;
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
  public void initialize(
      Context context,
      final InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      String errorMessage =
          createAdapterError(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "MoPub SDK requires an Activity context to initialize.");
      initializationCompleteCallback.onInitializationFailed(errorMessage);
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
      String errorMessage =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Initialization failed: Missing or Invalid MoPub Ad Unit ID.");
      initializationCompleteCallback.onInitializationFailed(errorMessage);
      return;
    }

    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(adUnitID).build();
    MoPubSingleton.getInstance()
        .initializeMoPubSDK(
            context,
            sdkConfiguration,
            new SdkInitializationListener() {
              @Override
              public void onInitializationFinished() {
                initializationCompleteCallback.onInitializationSucceeded();
              }
            });
  }

  @Override
  public void loadRewardedAd(
      final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    adUnitID = serverParameters.getString(MOPUB_AD_UNIT_KEY);

    if (TextUtils.isEmpty(adUnitID)) {
      String errorMessage =
          createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to request ad from MoPub: Missing or Invalid MoPub Ad Unit ID.");
      Log.w(TAG, errorMessage);
      mediationAdLoadCallback.onFailure(errorMessage);
      return;
    }

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    if (mediationExtras != null) {
      customRewardData = mediationExtras.getString(BundleBuilder.ARG_CUSTOM_REWARD_DATA);
    }

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
  public void showAd(Context context) {
    if (adExpired && mRewardedAdCallback != null) {
      String errorMessage =
          createAdapterError(
              ERROR_AD_EXPIRED,
              "Failed to show a MoPub rewarded video. "
                  + "The MoPub Ad has expired. Please make a new Ad Request.");
      mRewardedAdCallback.onAdFailedToShow(errorMessage);
    } else if (!adExpired) {
      boolean didShow = MoPubSingleton.getInstance().showRewardedAd(adUnitID, customRewardData);
      if (!didShow && mRewardedAdCallback != null) {
        String errorMessage =
            createAdapterError(
                ERROR_AD_NOT_READY,
                "MoPub does not have a rewarded ad ready to show for ad unit ID: " + adUnitID);
        Log.e(TAG, errorMessage);
        mRewardedAdCallback.onAdFailedToShow(errorMessage);
      }
    }
  }

  /** {@link MoPubRewardedVideoListener} implementation */
  @Override
  public void onAdFailedToLoad(int errorCode, String message) {
    String errorMessage = createAdapterError(errorCode, message);
    Log.e(TAG, errorMessage);

    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(errorMessage);
    }
  }

  @Override
  public void onRewardedVideoLoadSuccess(@NonNull String adUnitId) {
    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(MoPubMediationAdapter.this);
    }
  }

  @Override
  public void onRewardedVideoLoadFailure(
      @NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
    String errorSDKMessage = createSDKError(errorCode);
    Log.w(TAG, errorSDKMessage);

    // MoPub Rewarded video ads expire after 4 hours.
    if (errorCode == MoPubErrorCode.EXPIRED) {
      MoPubSingleton.getInstance().adExpired(adUnitId, MoPubMediationAdapter.this);
      adExpired = true;
    }

    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(errorSDKMessage);
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
  public void onRewardedVideoPlaybackError(
      @NonNull String adUnitId, @NonNull MoPubErrorCode errorCode) {
    String errorSDKMessage = createSDKError(errorCode);
    Log.i(TAG, "Failed to playback MoPub rewarded video: " + errorSDKMessage);

    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow(errorSDKMessage);
    }
  }

  @Override
  public void onRewardedVideoClicked(@NonNull String adUnitId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onRewardedVideoCompleted(
      @NonNull Set<String> adUnitIds, @NonNull final MoPubReward reward) {
    Preconditions.checkNotNull(reward);

    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoComplete();
      mRewardedAdCallback.onUserEarnedReward(
          new RewardItem() {
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
