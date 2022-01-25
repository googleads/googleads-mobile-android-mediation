package com.google.ads.mediation.mytarget;

import static com.google.ads.mediation.mytarget.MyTargetTools.handleMediationExtras;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import com.my.target.ads.Reward;
import com.my.target.ads.RewardedAd;
import com.my.target.ads.RewardedAd.RewardedAdListener;
import com.my.target.common.CustomParams;
import com.my.target.common.MyTargetVersion;
import java.util.List;

public class MyTargetMediationAdapter extends Adapter
    implements MediationRewardedAd, RewardedAdListener {

  static final String TAG = MyTargetMediationAdapter.class.getSimpleName();

  // region Error codes
  // MyTarget adapter error domain.
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mytarget";

  // MyTarget SDK error domain.
  public static final String MY_TARGET_SDK_ERROR_DOMAIN = "com.my.target.ads";

  /**
   * MyTarget SDK returned an error.
   */
  public static final int ERROR_MY_TARGET_SDK = 100;

  /**
   * Invalid server parameters (e.g. MyTarget Slot ID is missing).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a MyTarget supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Ad request is not a Unified native ad request.
   */
  public static final int ERROR_NON_UNIFIED_NATIVE_REQUEST = 103;

  /**
   * The loaded native ad from MyTarget is different from the requested native ad.
   */
  public static final int ERROR_INVALID_NATIVE_AD_LOADED = 104;

  /**
   * The loaded native ad from myTarget is missing some required assets (e.g. image or icon).
   */
  public static final int ERROR_MISSING_REQUIRED_NATIVE_ASSET = 105;
  // endregion

  private RewardedAd mRewardedAd;

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

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

    String logMessage = String
        .format("Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = MyTargetVersion.VERSION;
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
  public void initialize(Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    // MyTarget SDK does not have any API for initialization.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
    Log.d(TAG, "Requesting myTarget rewarded mediation with slot ID: " + slotId);

    if (slotId < 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    mAdLoadCallback = mediationAdLoadCallback;

    mRewardedAd = new RewardedAd(slotId, context);
    CustomParams params = mRewardedAd.getCustomParams();
    handleMediationExtras(TAG, mediationRewardedAdConfiguration.getMediationExtras(), params);
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY,
        MyTargetTools.PARAM_MEDIATION_VALUE);
    mRewardedAd.setListener(MyTargetMediationAdapter.this);
    mRewardedAd.load();
  }

  @Override
  public void showAd(Context context) {
    Log.d(TAG, "Showing video.");
    if (mRewardedAd != null) {
      mRewardedAd.show();
    } else if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow("Rewarded Video is null.");
    }
  }

  /**
   * A {@link RewardedAdListener} used to forward myTarget rewarded video events to Google.
   */
  @Override
  public void onLoad(@NonNull final RewardedAd ad) {
    Log.d(TAG, "Ad loaded.");
    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(MyTargetMediationAdapter.this);
    }
  }

  @Override
  public void onNoAd(@NonNull final String reason, @NonNull final RewardedAd ad) {
    AdError error = new AdError(ERROR_MY_TARGET_SDK, reason, MY_TARGET_SDK_ERROR_DOMAIN);
    Log.e(TAG, error.getMessage());
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onClick(@NonNull final RewardedAd ad) {
    Log.d(TAG, "Ad clicked.");
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onDismiss(@NonNull final RewardedAd ad) {
    Log.d(TAG, "Ad dismissed.");
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onReward(@NonNull Reward reward, @NonNull RewardedAd ad) {
    Log.d(TAG, "Rewarded.");
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onVideoComplete();
      mRewardedAdCallback.onUserEarnedReward(new MyTargetReward(reward));
    }
  }

  @Override
  public void onDisplay(@NonNull final RewardedAd ad) {
    Log.d(TAG, "Ad displayed.");
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdOpened();
      // myTarget has no callback for starting video, but rewarded video always
      // has autoplay param and starts immediately, so we can notify Google about this.
      mRewardedAdCallback.onVideoStart();
      mRewardedAdCallback.reportAdImpression();
    }
  }

  private static class MyTargetReward implements RewardItem {

    private final @NonNull
    String type;

    public MyTargetReward(@NonNull Reward reward) {
      this.type = reward.type;
    }

    @Override
    public @NonNull
    String getType() {
      return type;
    }

    @Override
    public int getAmount() {
      return 1;
    }
  }
}
