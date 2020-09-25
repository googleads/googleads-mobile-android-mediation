package com.google.ads.mediation.nend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
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
import java.util.List;
import net.nend.android.NendAdRewardItem;
import net.nend.android.NendAdRewardedListener;
import net.nend.android.NendAdRewardedVideo;
import net.nend.android.NendAdVideo;

/** The {@link NendMediationAdapter} to load and show Nend rewarded video ads. */
public class NendMediationAdapter extends Adapter
    implements MediationNativeAdapter, MediationRewardedAd, NendAdRewardedListener {

  static final String TAG = NendMediationAdapter.class.getSimpleName();

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

  /** {@link Adapter} implementation */
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
    String versionString = net.nend.android.BuildConfig.VERSION_NAME;
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
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> mediationConfigurations) {

    // Nend SDK does not have any API for initialization.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public void loadRewardedAd(
      MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {

    Context context = mediationRewardedAdConfiguration.getContext();

    if (!(context instanceof Activity)) {
      String logMessage =
          "Failed to request ad from Nend: Nend requires an Activity context to load an ad.";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    Bundle networkExtras = mediationRewardedAdConfiguration.getMediationExtras();

    String apiKey = serverParameters.getString(KEY_API_KEY);
    if (TextUtils.isEmpty(apiKey)) {
      String logMessage = "Failed to request ad from Nend: Missing or Invalid API Key.";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    int spotID = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
    if (spotID <= 0) {
      String logMessage = "Failed to request ad from Nend: Missing or invalid Spot ID";
      Log.w(TAG, logMessage);
      mediationAdLoadCallback.onFailure(logMessage);
      return;
    }

    mAdLoadCallback = mediationAdLoadCallback;

    mRewardedVideo = new NendAdRewardedVideo(context, spotID, apiKey);
    mRewardedVideo.setAdListener(NendMediationAdapter.this);
    mRewardedVideo.setMediationName(MEDIATION_NAME_ADMOB);
    if (networkExtras != null) {
      mRewardedVideo.setUserId(networkExtras.getString(KEY_USER_ID, ""));
    }
    mRewardedVideo.loadAd();
  }

  @Override
  public void showAd(Context context) {
    if (mRewardedVideo.isLoaded()) {
      if (context instanceof Activity) {
        mRewardedVideo.showAd((Activity) context);
      } else if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow("Nend Ads require an Activity context to show ads.");
      }
    } else if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow("Ad not ready yet.");
    }
  }

  /** {@link MediationNativeAdapter} implementation */
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
  public void requestNativeAd(
      Context context,
      MediationNativeListener mediationNativeListener,
      Bundle serverParameters,
      NativeMediationAdRequest nativeMediationAdRequest,
      Bundle mediationExtras) {
    nativeAdForwarder = new NendNativeAdForwarder(NendMediationAdapter.this);
    nativeAdForwarder.requestNativeAd(
        context,
        mediationNativeListener,
        serverParameters,
        nativeMediationAdRequest,
        mediationExtras);
  }

  /** {@link NendAdRewardedListener} implementation */
  @Override
  public void onLoaded(@NonNull NendAdVideo nendAdVideo) {
    if (mAdLoadCallback != null) {
      mRewardedAdCallback = mAdLoadCallback.onSuccess(NendMediationAdapter.this);
    }
  }

  @Override
  public void onFailedToLoad(@NonNull NendAdVideo nendAdVideo, int errorCode) {
    String logMessage =
        "Failed to request ad from Nend, Error Code: "
            + ErrorUtil.convertErrorCodeFromNendVideoToAdMob(errorCode);
    Log.w(TAG, logMessage);
    if (mAdLoadCallback != null) {
      mAdLoadCallback.onFailure(logMessage);
    }
    mRewardedVideo.releaseAd();
  }

  @Override
  public void onRewarded(
      @NonNull NendAdVideo nendAdVideo, @NonNull NendAdRewardItem nendAdRewardItem) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onUserEarnedReward(new NendMediationRewardItem(nendAdRewardItem));
    }
  }

  @Override
  public void onFailedToPlay(@NonNull NendAdVideo nendAdVideo) {
    if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow("Internal Error.");
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
