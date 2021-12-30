package com.google.ads.mediation.chartboost;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;

public class ChartboostMediationAdapter extends Adapter implements MediationRewardedAd {

  static final String TAG = ChartboostMediationAdapter.class.getSimpleName();

  // region Error codes
  // Chartboost adapter error domain.
  static final String ERROR_DOMAIN = "com.google.ads.mediation.chartboost";

  // Chartboost SDK error domain.
  static final String CHARTBOOST_SDK_ERROR_DOMAIN = "com.chartboost.sdk";

  /**
   * Chartboost adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_AD_ALREADY_LOADED,
          ERROR_INVALID_SERVER_PARAMETERS
      })
  public @interface AdapterError {

  }

  /**
   * The requested ad size does not match a Chartboost supported banner size.
   */
  static final int ERROR_BANNER_SIZE_MISMATCH = 100;

  /**
   * Chartboost can only load 1 ad per location at a time.
   */
  static final int ERROR_AD_ALREADY_LOADED = 101;

  /**
   * Invalid server parameters (e.g. Chartboost App ID is missing).
   */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 102;
  // endregion

  /**
   * A Chartboost extras object used to store optional information used when loading ads.
   */
  private ChartboostParams mChartboostParams = new ChartboostParams();

  /**
   * Flag to keep track of whether or not this {@link ChartboostMediationAdapter} is loading ads.
   */
  private boolean mIsLoading;

  private InitializationCompleteCallback mInitializationCallback;
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  /**
   * {@link Adapter} implementation
   */
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
    String versionString = Chartboost.getSDKVersion();
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

    HashMap<String, Bundle> chartboostConfigs = new HashMap<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle params = configuration.getServerParameters();
      String serverAppID = params.getString(ChartboostAdapterUtils.KEY_APP_ID);

      if (!TextUtils.isEmpty(serverAppID)) {
        chartboostConfigs.put(serverAppID, params);
      }
    }

    String appID;
    Bundle serverParameters;
    int count = chartboostConfigs.size();
    if (count <= 0) {
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid App ID.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }

    appID = chartboostConfigs.keySet().iterator().next();
    serverParameters = chartboostConfigs.get(appID);

    // Multiple app IDs are not considered an error.
    if (count > 1) {
      String logMessage =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the Chartboost SDK.",
              ChartboostAdapterUtils.KEY_APP_ID, chartboostConfigs.keySet(), appID);
      Log.w(TAG, logMessage);
    }

    mInitializationCallback = initializationCompleteCallback;
    mChartboostParams = ChartboostAdapterUtils.createChartboostParams(serverParameters, null);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send initialization failed event.
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Invalid server parameters.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.toString());
      return;
    }
    ChartboostSingleton.startChartboostRewardedVideo(context, mChartboostRewardedVideoDelegate);
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    mAdLoadCallback = mediationAdLoadCallback;
    final Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    final Bundle extras = mediationRewardedAdConfiguration.getMediationExtras();

    mChartboostParams = ChartboostAdapterUtils.createChartboostParams(serverParameters, extras);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send initialization failed event.
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Invalid server parameters.", ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationRewardedAdConfiguration.getContext();
    ChartboostSingleton.startChartboostRewardedVideo(context, mChartboostRewardedVideoDelegate);
  }

  @Override
  public void showAd(@NonNull Context context) {
    ChartboostSingleton.showRewardedVideoAd(mChartboostRewardedVideoDelegate);
  }

  /**
   * The Abstract Chartboost adapter delegate used to forward events received from {@link
   * ChartboostSingleton} to Google Mobile Ads SDK for rewarded video ads.
   */
  private final AbstractChartboostAdapterDelegate mChartboostRewardedVideoDelegate =
      new AbstractChartboostAdapterDelegate() {

        @Override
        public ChartboostParams getChartboostParams() {
          return mChartboostParams;
        }

        @Override
        public void didInitialize() {
          super.didInitialize();
          if (mInitializationCallback != null) {
            mInitializationCallback.onInitializationSucceeded();
          }

          // If 'mAdLoadCallback' is not null, then it means an Ad request is pending
          // to be sent after initializing.
          if (mAdLoadCallback != null) {
            mIsLoading = true;
            ChartboostSingleton.loadRewardedVideoAd(mChartboostRewardedVideoDelegate);
          }
        }

        @Override
        public void didCacheRewardedVideo(String location) {
          super.didCacheRewardedVideo(location);
          if (mAdLoadCallback != null
              && mIsLoading
              && location.equals(mChartboostParams.getLocation())) {
            mIsLoading = false;
            mRewardedAdCallback = mAdLoadCallback.onSuccess(ChartboostMediationAdapter.this);
          }
        }

        @Override
        public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
          super.didFailToLoadRewardedVideo(location, error);
          AdError loadError = ChartboostAdapterUtils.createSDKError(error);
          Log.i(TAG, loadError.toString());

          if (mAdLoadCallback != null && location.equals(mChartboostParams.getLocation())) {
            if (mIsLoading) {
              mAdLoadCallback.onFailure(loadError);
              mIsLoading = false;
            } else if (error == CBError.CBImpressionError.INTERNET_UNAVAILABLE_AT_SHOW) {
              // Chartboost sends the CBErrorInternetUnavailableAtShow error when
              // the Chartboost SDK fails to show an ad because no network connection
              // is available.
              if (mRewardedAdCallback != null) {
                mRewardedAdCallback.onAdFailedToShow(loadError);
              }
            }
          }
        }

        @Override
        public void onAdFailedToLoad(@NonNull AdError loadError) {
          Log.e(TAG, loadError.toString());
          if (mAdLoadCallback != null) {
            mAdLoadCallback.onFailure(loadError);
          }
        }

        @Override
        public void didDismissRewardedVideo(String location) {
          super.didDismissRewardedVideo(location);
          if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdClosed();
          }
        }

        @Override
        public void didClickRewardedVideo(String location) {
          super.didClickRewardedVideo(location);
          if (mRewardedAdCallback != null) {
            mRewardedAdCallback.reportAdClicked();
          }
        }

        @Override
        public void didCompleteRewardedVideo(String location, int reward) {
          super.didCompleteRewardedVideo(location, reward);
          if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onVideoComplete();
            mRewardedAdCallback.onUserEarnedReward(new ChartboostReward(reward));
          }
        }

        @Override
        public void didDisplayRewardedVideo(String location) {
          super.didDisplayRewardedVideo(location);
          if (mRewardedAdCallback != null) {
            // Charboost doesn't have a video started callback. We assume that the video
            // started once the ad has been displayed.
            mRewardedAdCallback.onAdOpened();
            mRewardedAdCallback.onVideoStart();
            mRewardedAdCallback.reportAdImpression();
          }
        }
      };
}
