package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.inmobi.sdk.InMobiSdk;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InMobi Adapter for AdMob Mediation used to load and show rewarded video ads. This class should
 * not be used directly by publishers.
 */
public class InMobiMediationAdapter extends Adapter {

  public static final String TAG = InMobiMediationAdapter.class.getSimpleName();

  // region Error codes
  // InMobi adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.inmobi";

  // InMobi SDK error domain.
  public static final String INMOBI_SDK_ERROR_DOMAIN = "com.inmobi.sdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_INMOBI_FAILED_INITIALIZATION,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_NON_UNIFIED_NATIVE_REQUEST,
          ERROR_INMOBI_NOT_INITIALIZED,
          ERROR_AD_NOT_READY,
          ERROR_AD_DISPLAY_FAILED,
          ERROR_MISSING_NATIVE_ASSETS,
          ERROR_MALFORMED_IMAGE_URL,
          ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,
      })
  public @interface AdapterError {

  }

  /**
   * Invalid server parameters (e.g. InMobi Account ID is missing).
   */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 100;

  /**
   * Failed to initialize the InMobi SDK.
   */
  static final int ERROR_INMOBI_FAILED_INITIALIZATION = 101;

  /**
   * The requested ad size does not match an InMobi supported banner size.
   */
  static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Ad request is not a Unified native ad request.
   */
  static final int ERROR_NON_UNIFIED_NATIVE_REQUEST = 103;

  /**
   * Attempted to request an InMobi ad without initializing the InMobi SDK. This should not happen
   * since the adapter initializes the InMobi SDK prior to requesting InMobi ads.
   */
  static final int ERROR_INMOBI_NOT_INITIALIZED = 104;

  /**
   * InMobi's ad is not yet ready to be shown.
   */
  static final int ERROR_AD_NOT_READY = 105;

  /**
   * InMobi failed to display an ad.
   */
  static final int ERROR_AD_DISPLAY_FAILED = 106;

  /**
   * InMobi returned a native ad with a missing required asset.
   */
  static final int ERROR_MISSING_NATIVE_ASSETS = 107;

  /**
   * InMobi's native ad image assets contain a malformed URL.
   */
  static final int ERROR_MALFORMED_IMAGE_URL = 108;

  /**
   * The adapter failed to download InMobi's native ad image assets.
   */
  static final int ERROR_NATIVE_ASSET_DOWNLOAD_FAILED = 109;
  // endregion

  // Flag to check whether the InMobi SDK has been initialized or not.
  static AtomicBoolean isSdkInitialized = new AtomicBoolean();

  // Callback listener
  private InMobiRewardedAd mInMobiRewarded;

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

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = InMobiSdk.getVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String
        .format("Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      final @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (isSdkInitialized.get()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    HashSet<String> accountIDs = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      String serverAccountID = configuration.getServerParameters()
          .getString(InMobiAdapterUtils.KEY_ACCOUNT_ID);

      if (!TextUtils.isEmpty(serverAccountID)) {
        accountIDs.add(serverAccountID);
      }
    }

    int count = accountIDs.size();
    if (count <= 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Account ID.",
          ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(error.getMessage());
      return;
    }

    String accountID = accountIDs.iterator().next();

    if (count > 1) {
      String message = String.format("Multiple '%s' entries found: %s. "
              + "Using '%s' to initialize the InMobi SDK",
          InMobiAdapterUtils.KEY_ACCOUNT_ID, accountIDs, accountID);
      Log.w(TAG, message);
    }

    InMobiInitializer.getInstance().init(context, accountID, new Listener() {
      @Override
      public void onInitializeSuccess() {
        isSdkInitialized.set(true);
        initializationCompleteCallback.onInitializationSucceeded();
      }

      @Override
      public void onInitializeError(@NonNull AdError error) {
        // TODO: Forward the AdError object when available.
        initializationCompleteCallback.onInitializationFailed(error.getMessage());
      }
    });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      final @NonNull MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    mInMobiRewarded = new InMobiRewardedAd(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    mInMobiRewarded.load();
  }

}
