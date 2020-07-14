package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IronSourceMediationAdapter extends Adapter
    implements MediationRewardedAd, IronSourceAdapterListener {

  // region Error codes
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_REQUIRES_ACTIVITY_CONTEXT,
        ERROR_AD_ALREADY_LOADED
      })
  public @interface AdapterError {}

  /** Server parameters (e.g. placement ID) are nil. */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /** IronSource requires an {@link Activity} context to initialize their SDK. */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 102;

  /** IronSource can only load 1 ad per IronSource instance ID. */
  public static final int ERROR_AD_ALREADY_LOADED = 103;
  // endregion

  private static AtomicBoolean mDidInitRewardedVideo = new AtomicBoolean(false);

  private static final List<IronSource.AD_UNIT> mAdUnitsToInit =
      new ArrayList<>(Collections.singletonList(IronSource.AD_UNIT.REWARDED_VIDEO));

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

  /** Holds the instance state */
  private INSTANCE_STATE mState = INSTANCE_STATE.START;

  /** This is the id of the rewarded video instance requested. */
  private String mInstanceID;

  INSTANCE_STATE getInstanceState() {
    return mState;
  }

  void setInstanceState(INSTANCE_STATE mState) {
    this.mState = mState;
  }

  enum INSTANCE_STATE {
    START, // Initial state when instance wasn't loaded yet
    CAN_LOAD, // If load is called on an instance with this state, pass it forward to IronSource SDK
    LOCKED, // if load is called on an instance with this state, report load fail
  }

  /** MediationRewardedAd implementation. */
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

  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.VERSION_NAME;
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
  public void initialize(
      Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {
    if (mDidInitRewardedVideo.get()) {
      initializationCompleteCallback.onInitializationSucceeded();
      return;
    }

    if (!(context instanceof Activity)) {
      // Context not an Activity context, log the reason for failure and fail the
      // initialization.
      String adapterError =
          IronSourceAdapterUtils.createAdapterError(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "IronSource SDK requires an Activity context to initialize.");
      initializationCompleteCallback.onInitializationFailed(adapterError);
      return;
    }

    HashSet<String> appKeys = new HashSet<>();
    for (MediationConfiguration configuration : mediationConfigurations) {
      Bundle serverParameters = configuration.getServerParameters();
      String appKeyFromServer = serverParameters.getString(IronSourceAdapterUtils.KEY_APP_KEY);

      if (!TextUtils.isEmpty(appKeyFromServer)) {
        appKeys.add(appKeyFromServer);
      }
    }

    int count = appKeys.size();
    if (count <= 0) {
      String adapterError =
          IronSourceAdapterUtils.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App Key.");
      initializationCompleteCallback.onInitializationFailed(adapterError);
      return;
    }

    String appKey = appKeys.iterator().next();
    if (count > 1) {
      String message =
          String.format(
              "Multiple '%s' entries found: %s. Using '%s' to initialize the IronSource SDK.",
              IronSourceAdapterUtils.KEY_APP_KEY, appKeys.toString(), appKey);
      Log.w(TAG, message);
    }

    if (!mDidInitRewardedVideo.getAndSet(true)) {
      IronSourceManager.getInstance().initIronSourceSDK((Activity) context, appKey, mAdUnitsToInit);
    }

    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    if (serverParameters == null) {
      String adapterError =
          IronSourceAdapterUtils.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing server parameters.");
      Log.e(TAG, adapterError);
      mediationAdLoadCallback.onFailure(adapterError);
      return;
    }

    this.mInstanceID =
        serverParameters.getString(IronSourceAdapterUtils.KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
    if (!mDidInitRewardedVideo.getAndSet(true)) {
      Context context = mediationRewardedAdConfiguration.getContext();
      if (!(context instanceof Activity)) {
        // Context not an Activity context, log the reason for failure and fail the
        // initialization.
        String adapterError =
            IronSourceAdapterUtils.createAdapterError(
                ERROR_REQUIRES_ACTIVITY_CONTEXT,
                "IronSource SDK requires an Activity context to initialize.");
        Log.e(TAG, adapterError);
        mediationAdLoadCallback.onFailure(adapterError);
        return;
      }

      String appKey = serverParameters.getString(KEY_APP_KEY);
      if (TextUtils.isEmpty(appKey)) {
        String adapterError =
            IronSourceAdapterUtils.createAdapterError(
                ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App Key.");
        Log.e(TAG, adapterError);
        mediationAdLoadCallback.onFailure(adapterError);
        return;
      }

      IronSourceManager.getInstance().initIronSourceSDK((Activity) context, appKey, mAdUnitsToInit);
    }

    mMediationAdLoadCallback = mediationAdLoadCallback;
    Log.d(
        TAG,
        String.format("Loading IronSource rewarded ad with instance ID: %s", this.mInstanceID));
    IronSourceManager.getInstance()
        .loadRewardedVideo(this.mInstanceID, new WeakReference<>(IronSourceMediationAdapter.this));
  }

  @Override
  public void showAd(Context context) {
    Log.d(
        TAG, String.format("Showing IronSource rewarded ad for instance ID: %s", this.mInstanceID));
    IronSourceManager.getInstance().showRewardedVideo(this.mInstanceID);
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
    final String sdkError = IronSourceAdapterUtils.createSDKError(ironSourceError);
    Log.w(TAG, sdkError);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(sdkError);
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
    final String sdkError = IronSourceAdapterUtils.createSDKError(ironSourceError);
    Log.w(TAG, sdkError);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdFailedToShow(sdkError);
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
  public void onAdFailedToLoad(int errorCode, @NonNull String errorMessage) {
    final String adapterError = IronSourceAdapterUtils.createAdapterError(errorCode, errorMessage);
    Log.w(TAG, adapterError);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(adapterError);
            }
          }
        });
  }
  // endregion

  /** A {@link RewardItem} used to map IronSource reward to Google's reward. */
  static class IronSourceReward implements RewardItem {

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
