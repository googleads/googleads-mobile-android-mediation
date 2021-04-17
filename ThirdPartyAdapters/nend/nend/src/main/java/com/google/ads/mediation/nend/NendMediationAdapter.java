package com.google.ads.mediation.nend;

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
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.VersionInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import net.nend.android.NendAdInterstitial.NendAdInterstitialShowResult;
import net.nend.android.NendAdInterstitial.NendAdInterstitialStatusCode;
import net.nend.android.NendAdRewardItem;
import net.nend.android.NendAdRewardedActionListener;
import net.nend.android.NendAdRewardedVideo;
import net.nend.android.NendAdVideo;
import net.nend.android.NendAdVideoPlayingState;
import net.nend.android.NendAdVideoPlayingStateListener;
import net.nend.android.NendAdVideoType;
import net.nend.android.NendAdView.NendError;

/**
 * The {@link NendMediationAdapter} to load and show Nend rewarded video ads.
 */
public class NendMediationAdapter extends Adapter
    implements MediationNativeAdapter, MediationRewardedAd, NendAdRewardedActionListener {

  static final String TAG = NendMediationAdapter.class.getSimpleName();

  // region Error codes
  // Nend adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.nend";

  // Nend SDK error domain.
  public static final String NEND_SDK_ERROR_DOMAIN = "net.nend.android";

  /**
   * Nend adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_REQUIRES_ACTIVITY_CONTEXT,
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_AD_NOT_READY,
      ERROR_AD_FAILED_TO_PLAY,
      ERROR_BANNER_SIZE_MISMATCH,
      ERROR_NULL_CONTEXT
  })
  public @interface AdapterError {

  }

  /**
   * Nend requires an activity context to load and show ads.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 101;

  /**
   * Missing or invalid server parameters (e.g. nend Spot ID is null).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * Ad is not yet ready to be shown.
   */
  public static final int ERROR_AD_NOT_READY = 103;

  /**
   * Nend failed to play the ad.
   */
  public static final int ERROR_AD_FAILED_TO_PLAY = 104;

  /**
   * The requested ad size does not match a nend supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 105;

  /**
   * The {@link Context} object reference is {@code null} and/or was recently released from memory.
   */
  public static final int ERROR_NULL_CONTEXT = 106;

  /**
   * Convert nend's {@link NendAdInterstitialStatusCode} to a mediation specific error code.
   *
   * @param statusCode nend's interstitial status code.
   * @return the mediation specific error code.
   */
  public static int getMediationErrorCode(@NonNull NendAdInterstitialStatusCode statusCode) {
    switch (statusCode) {
      case SUCCESS:
        return 200;
      case INVALID_RESPONSE_TYPE:
        return 201;
      case FAILED_AD_REQUEST:
        return 202;
      case FAILED_AD_DOWNLOAD:
        return 203;
    }
    // Error '299' to indicate that the error is new and has not been supported by the adapter yet.
    return 299;
  }

  /**
   * Convert nend's {@link NendAdInterstitialShowResult} to a mediation specific error code.
   *
   * @param statusCode nend's show result.
   * @return the mediation specific error code.
   */
  public static int getMediationErrorCode(@NonNull NendAdInterstitialShowResult statusCode) {
    switch (statusCode) {
      case AD_SHOW_SUCCESS:
        return 300;
      case AD_LOAD_INCOMPLETE:
        return 301;
      case AD_REQUEST_INCOMPLETE:
        return 302;
      case AD_DOWNLOAD_INCOMPLETE:
        return 303;
      case AD_FREQUENCY_NOT_REACHABLE:
        return 304;
      case AD_SHOW_ALREADY:
        return 305;
    }
    // Error '399' to indicate that the error is new and has not been supported by the adapter yet.
    return 399;
  }

  /**
   * Convert nend's {@link NendError} to a mediation specific error code.
   *
   * @param nendError nend's error object.
   * @return the mediation specific error code.
   */
  public static int getMediationErrorCode(@NonNull NendError nendError) {
    switch (nendError) {
      case AD_SIZE_TOO_LARGE:
        return 400;
      case INVALID_RESPONSE_TYPE:
        return 401;
      case FAILED_AD_REQUEST:
        return 402;
      case FAILED_AD_DOWNLOAD:
        return 403;
      case AD_SIZE_DIFFERENCES:
        return 404;
    }
    // Error '499' to indicate that the error is new and has not been supported by the adapter yet.
    return 499;
  }
  // endregion

  static final String KEY_USER_ID = "key_user_id";
  static final String KEY_API_KEY = "apiKey";
  static final String KEY_SPOT_ID = "spotId";

  static final String MEDIATION_NAME_ADMOB = "AdMob";

  private NendAdRewardedVideo mRewardedVideo;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  public enum FormatType {
    TYPE_VIDEO,
    TYPE_NORMAL
  }

  private NendNativeAdForwarder nativeAdForwarder;

  /**
   * {@link Adapter} implementation
   */
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
    String versionString = net.nend.android.BuildConfig.NEND_SDK_VERSION;
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
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    // Nend SDK does not have any API for initialization.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Nend requires an Activity context to load an ad.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    Bundle networkExtras = mediationRewardedAdConfiguration.getMediationExtras();

    String apiKey = serverParameters.getString(KEY_API_KEY);
    if (TextUtils.isEmpty(apiKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid API key.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    int spotID = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
    if (spotID <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid spot ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdLoadCallback = mediationAdLoadCallback;

    mRewardedVideo = new NendAdRewardedVideo(context, spotID, apiKey);
    mRewardedVideo.setActionListener(NendMediationAdapter.this);
    mRewardedVideo.setMediationName(MEDIATION_NAME_ADMOB);
    mRewardedVideo.setUserId(networkExtras.getString(KEY_USER_ID, ""));

    mRewardedVideo.loadAd();
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (!mRewardedVideo.isLoaded()) {
      AdError error = new AdError(ERROR_AD_NOT_READY, "nend rewarded ad not ready yet.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "nend requires an Activity context to show ads.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    mRewardedVideo.showAd((Activity) context);
  }

  /**
   * {@link MediationNativeAdapter} implementation
   */
  @Override
  public void onResume() {
    if (nativeAdForwarder != null) {
      nativeAdForwarder.onResume();
    }
  }

  @Override
  public void onPause() {
    if (nativeAdForwarder != null) {
      nativeAdForwarder.onPause();
    }
  }

  @Override
  public void onDestroy() {
    if (nativeAdForwarder != null) {
      nativeAdForwarder.onDestroy();
      nativeAdForwarder = null;
    }
  }

  @Override
  public void requestNativeAd(@NonNull Context context,
      @NonNull MediationNativeListener mediationNativeListener,
      @NonNull Bundle serverParameters, @NonNull NativeMediationAdRequest nativeMediationAdRequest,
      @Nullable Bundle mediationExtras) {
    nativeAdForwarder = new NendNativeAdForwarder(NendMediationAdapter.this);
    nativeAdForwarder.requestNativeAd(context, mediationNativeListener, serverParameters,
        nativeMediationAdRequest, mediationExtras);
  }

  /**
   * {@link NendAdRewardedActionListener} implementation
   */
  @Override
  public void onLoaded(@NonNull NendAdVideo nendAdVideo) {
    if (mRewardedVideo.getType() == NendAdVideoType.NORMAL) {
      NendAdVideoPlayingState state = mRewardedVideo.playingState();
      if (state != null) {
        state.setPlayingStateListener(new NendAdVideoPlayingStateListener() {
          @Override
          public void onStarted(@NonNull NendAdVideo nendAdVideo) {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onVideoStart();
            }
          }

          @Override
          public void onStopped(@NonNull NendAdVideo nendAdVideo) {
            // No relevant event to forward to the Google Mobile Ads SDK.
          }

          @Override
          public void onCompleted(@NonNull NendAdVideo nendAdVideo) {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onVideoComplete();
            }
          }
        });
      }
    }

    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(NendMediationAdapter.this);
    }
  }

  @Override
  public void onFailedToLoad(@NonNull NendAdVideo nendAdVideo, int errorCode) {
    String errorMessage = String
        .format("Nend SDK returned an ad load failure callback with code: %d", errorCode);
    AdError error = new AdError(errorCode, errorMessage, NEND_SDK_ERROR_DOMAIN);
    Log.e(TAG, error.getMessage());
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(error);
    }
    mRewardedVideo.releaseAd();
  }

  @Override
  public void onRewarded(@NonNull NendAdVideo nendAdVideo,
      @NonNull NendAdRewardItem nendAdRewardItem) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onUserEarnedReward(new NendMediationRewardItem(nendAdRewardItem));
    }
  }

  @Override
  public void onFailedToPlay(@NonNull NendAdVideo nendAdVideo) {
    AdError error = new AdError(ERROR_AD_FAILED_TO_PLAY,
        "Nend SDK returned the onFailedToPlay() error callback.", ERROR_DOMAIN);
    Log.e(TAG, error.getMessage());
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onShown(@NonNull NendAdVideo nendAdVideo) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      mRewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onClosed(@NonNull NendAdVideo nendAdVideo) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
    mRewardedVideo.releaseAd();
  }

  @Override
  public void onAdClicked(@NonNull NendAdVideo nendAdVideo) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onInformationClicked(@NonNull NendAdVideo nendAdVideo) {
    // No relevant event to forward to the Google Mobile Ads SDK.
  }
}
