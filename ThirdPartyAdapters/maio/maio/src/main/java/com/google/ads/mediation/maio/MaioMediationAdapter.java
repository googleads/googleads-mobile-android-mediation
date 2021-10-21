package com.google.ads.mediation.maio;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.mediation.admob.adapter.BuildConfig;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;

public class MaioMediationAdapter extends Adapter
    implements MediationRewardedAd, MaioAdsManagerListener {

  public static final String TAG = MaioMediationAdapter.class.getSimpleName();

  protected String mMediaID;
  protected String mZoneID;

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  /**
   * Maio adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.maio";

  /**
   * Maio sdk error domain.
   */
  public static final String MAIO_SDK_ERROR_DOMAIN = "jp.maio.sdk.android";

  @IntDef(value = {ERROR_AD_NOT_AVAILABLE,
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_REQUIRES_ACTIVITY_CONTEXT,
  })

  @Retention(RetentionPolicy.SOURCE)
  public @interface AdapterError {

  }

  public static AdError getAdError(FailNotificationReason reason) {
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 99;
    switch (reason) {
      case AD_STOCK_OUT:
        code = 0;
        break;
      case NETWORK_NOT_READY:
        code = 1;
        break;
      case RESPONSE:
        code = 2;
        break;
      case NETWORK:
        code = 3;
        break;
      case UNKNOWN:
        code = 4;
        break;
      case VIDEO:
        code = 5;
        break;
    }
    return new AdError(code, "Failed to request ad from Maio: " + reason.toString(),
        MAIO_SDK_ERROR_DOMAIN);
  }

  /**
   * Maio does not have an ad available.
   */
  public static final int ERROR_AD_NOT_AVAILABLE = 101;

  /**
   * Invalid or missing server parameters.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * Activity context is required.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;


  /**
   * {@link Adapter} implementation
   */
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
        String.format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = MaioAds.getSdkVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(
      Context context,
      final InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed(
          "Maio SDK requires an Activity context to initialize");
      return;
    }

    HashSet<String> mediaIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String mediaIDFromServer =
          configuration.getServerParameters().getString(MaioAdsManager.KEY_MEDIA_ID);

      if (!TextUtils.isEmpty(mediaIDFromServer)) {
        mediaIDs.add(mediaIDFromServer);
      }
    }

    int count = mediaIDs.size();
    if (count > 0) {
      String mediaID = mediaIDs.iterator().next();

      if (count > 1) {
        String logMessage =
            String.format(
                "Multiple '%s' entries found: %s. Using '%s' to initialize the Maio SDK",
                MaioAdsManager.KEY_MEDIA_ID, mediaIDs, mediaID);
        Log.w(TAG, logMessage);
      }

      MaioAdsManager.getManager(mediaID)
          .initialize(
              (Activity) context,
              new MaioAdsManager.InitializationListener() {
                @Override
                public void onMaioInitialized() {
                  initializationCompleteCallback.onInitializationSucceeded();
                }
              });
    } else {
      initializationCompleteCallback.onInitializationFailed(
          "Initialization Failed: Missing or Invalid Media ID.");
    }
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Maio SDK requires an Activity context to load ads.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);

      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    mMediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mMediaID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Media ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);

      return;
    }

    mZoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(mZoneID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid Zone ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);

      return;
    }

    mAdLoadCallback = mediationAdLoadCallback;
    MaioAds.setAdTestMode(mediationRewardedAdConfiguration.isTestRequest());
    MaioAdsManager.getManager(mMediaID)
        .initialize(
            (Activity) context,
            new MaioAdsManager.InitializationListener() {
              @Override
              public void onMaioInitialized() {
                MaioAdsManager.getManager(mMediaID).loadAd(mZoneID, MaioMediationAdapter.this);
              }
            });
  }

  @Override
  public void showAd(Context context) {
    MaioAdsManager.getManager(mMediaID).showAd(mZoneID, MaioMediationAdapter.this);
  }

  // region MaioAdsManagerListener implementation
  @Override
  public void onInitialized() {
    // Not called.
    // MaioAdsManager calls MaioAdsManager.InitializationListener.onMaioInitialized() instead.
  }

  @Override
  public void onChangedCanShow(String zoneId, boolean isAvailable) {
    if (mAdLoadCallback != null && isAvailable) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(MaioMediationAdapter.this);
    }
  }

  @Override
  public void onFailed(FailNotificationReason reason, String zoneId) {
    AdError error = MaioMediationAdapter.getAdError(reason);
    Log.w(TAG, error.getMessage());
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdFailedToLoad(@NonNull AdError error) {
    Log.w(TAG, error.getMessage());
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdFailedToShow(@NonNull AdError error) {
    Log.w(TAG, error.getMessage());
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onOpenAd(String zoneId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      mRewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onStartedAd(String zoneId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoStart();
    }
  }

  @Override
  public void onClickedAd(String zoneId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoComplete();
      if (!skipped) {
        mRewardedAdCallback.onUserEarnedReward(new MaioReward());
      }
    }
  }

  @Override
  public void onClosedAd(String zoneId) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
  }
  // endregion

  /**
   * A {@link RewardItem} used to map maio rewards to Google's rewarded video ads rewards.
   */
  private class MaioReward implements RewardItem {

    private MaioReward() {
    }

    @Override
    public int getAmount() {
      return 1;
    }

    @Override
    public String getType() {
      return "";
    }
  }
}