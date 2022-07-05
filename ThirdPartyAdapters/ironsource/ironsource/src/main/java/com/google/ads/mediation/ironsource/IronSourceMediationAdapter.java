package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.ads.mediation.ironsource.IronSourceManager.InitializationCallback;
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
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;

public class IronSourceMediationAdapter extends Adapter
    implements MediationRewardedAd, IronSourceAdapterListener {

  // region Error codes
  // IronSource adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.ironsource";

  // IronSource SDK error domain.
  public static final String IRONSOURCE_SDK_ERROR_DOMAIN = "com.ironsource.mediationsdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          ERROR_AD_ALREADY_LOADED,
          ERROR_AD_SHOW_UNAUTHORIZED,
      })
  public @interface AdapterError {

  }

  /**
   * Server parameters (e.g. placement ID) are nil.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * IronSource requires an {@link Activity} context to initialize their SDK.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 102;

  /**
   * IronSource can only load 1 ad per IronSource instance ID.
   */
  public static final int ERROR_AD_ALREADY_LOADED = 103;

  /**
   * IronSource adapter does not have authority to show an ad instance.
   */
  public static final int ERROR_AD_SHOW_UNAUTHORIZED = 104;
  // endregion

  /**
   * Mediation listener used to forward rewarded ad events from IronSource SDK to Google Mobile Ads
   * SDK while ad is presented
   */
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  /**
   * Mediation listener used to forward rewarded ad events from IronSource SDK to Google Mobile Ads
   * SDK for loading phases of the ad
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;

  /**
   * This is the id of the rewarded video instance requested.
   */
  private String mInstanceID;

  /**
   * MediationRewardedAd implementation.
   */
  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = IronSourceUtils.getSDKVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      if (splits.length >= 4) {
        micro = micro * 100 + Integer.parseInt(splits[3]);
      }

      return new VersionInfo(major, minor, micro);
    }

    String logMessage =
        String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
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
      if (splits.length >= 5) {
        micro = micro * 100 + Integer.parseInt(splits[4]);
      }

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
  public void initialize(@NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    HashSet<String> appKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appKeyFromServer = serverParameters.getString(KEY_APP_KEY);

      if (!TextUtils.isEmpty(appKeyFromServer)) {
        appKeys.add(appKeyFromServer);
      }
    }

    int count = appKeys.size();
    if (count <= 0) {
      AdError initializationError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid app key.", ERROR_DOMAIN);
      initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
      return;
    }

    // Having multiple app keys is not considered an error.
    String appKey = appKeys.iterator().next();
    if (count > 1) {
      String message = String
          .format("Multiple '%s' entries found: %s. Using '%s' to initialize the IronSource SDK.",
              KEY_APP_KEY, appKeys, appKey);
      Log.w(TAG, message);
    }

    IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
        new IronSourceManager.InitializationCallback() {
          @Override
          public void onInitializeSuccess() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeError(@NonNull AdError initializationError) {
            initializationCompleteCallback.onInitializationFailed(initializationError.getMessage());
          }
        });
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    Context context = mediationRewardedAdConfiguration.getContext();
    String appKey = serverParameters.getString(KEY_APP_KEY);
    this.mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);

    IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
        new InitializationCallback() {
          @Override
          public void onInitializeSuccess() {
            mMediationAdLoadCallback = mediationAdLoadCallback;
            Log.d(TAG,
                String.format("Loading IronSource rewarded ad with instance ID: %s", mInstanceID));
            IronSourceManager.getInstance()
                .loadRewardedVideo(mInstanceID, IronSourceMediationAdapter.this);
          }

          @Override
          public void onInitializeError(@NonNull AdError initializationError) {
            Log.e(TAG, initializationError.getMessage());
            mediationAdLoadCallback.onFailure(initializationError);
          }
        });
  }

  @Override
  public void loadRewardedInterstitialAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    // IronSource Rewarded Interstitial ads use the same Rewarded Video API.
    Log.d(TAG, "IronSource adapter was asked to load a rewarded interstitial ad. "
        + "Using the rewarded ad request flow to load the ad to attempt to load a "
        + "rewarded interstitial ad from IronSource.");
    loadRewardedAd(mediationRewardedAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void showAd(@NonNull Context context) {
    Log.d(TAG,
        String.format("Showing IronSource rewarded ad for instance ID: %s", this.mInstanceID));
    IronSourceManager.getInstance()
        .showRewardedVideo(this.mInstanceID, IronSourceMediationAdapter.this);
  }

  // region ISDemandOnlyRewardedVideoListener implementation.
  public void onRewardedVideoAdLoadSuccess(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad loaded for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationRewardedAdCallback =
                  mMediationAdLoadCallback.onSuccess(IronSourceMediationAdapter.this);
            }
          }
        });
  }

  public void onRewardedVideoAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
    AdError loadError = new AdError(ironSourceError.getErrorCode(),
        ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage = String
        .format("IronSource failed to load rewarded ad for instance ID: %s. Error: %s", instanceId,
            loadError.getMessage());
    Log.e(TAG, errorMessage);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(loadError);
            }
          }
        });
  }

  public void onRewardedVideoAdOpened(final String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad opened for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdOpened();
              mMediationRewardedAdCallback.onVideoStart();
              mMediationRewardedAdCallback.reportAdImpression();
            }
          }
        });
  }

  public void onRewardedVideoAdClosed(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad closed for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdClosed();
            }
          }
        });
  }

  public void onRewardedVideoAdRewarded(String instanceId) {
    final IronSourceReward reward = new IronSourceReward();
    Log.d(
        TAG,
        String.format(
            "IronSource rewarded ad received reward: %d %s, for instance ID: %s",
            reward.getAmount(), reward.getType(), instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onVideoComplete();
              mMediationRewardedAdCallback.onUserEarnedReward(reward);
            }
          }
        });
  }

  public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
    AdError showError = new AdError(ironSourceError.getErrorCode(),
        ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage = String
        .format("IronSource failed to show rewarded ad for instance ID: %s. Error: %s", instanceId,
            showError.getMessage());
    Log.e(TAG, errorMessage);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdFailedToShow(showError);
            }
          }
        });
  }

  public void onRewardedVideoAdClicked(String instanceId) {
    Log.d(TAG, String.format("IronSource rewarded ad clicked for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.reportAdClicked();
            }
          }
        });
  }
  // endregion

  // region IronSourceAdapterListener implementation.
  @Override
  public void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.getMessage());
    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(loadError);
            }
          }
        });
  }

  @Override
  public void onAdFailedToShow(@NonNull AdError showError) {
    Log.e(TAG, showError.getMessage());
    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdFailedToShow(showError);
            }
          }
        });
  }
  // endregion

  /**
   * A {@link RewardItem} used to map IronSource reward to Google's reward.
   */
  static class IronSourceReward implements RewardItem {

    @NonNull
    @Override
    public String getType() {
      return "";
    }

    @Override
    public int getAmount() {
      return 1;
    }
  }
}
