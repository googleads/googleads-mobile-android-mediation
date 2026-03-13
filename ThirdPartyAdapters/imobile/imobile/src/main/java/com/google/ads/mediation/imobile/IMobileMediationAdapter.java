// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.imobile;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAdMapper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/** i-mobile mediation adapter. */
public class IMobileMediationAdapter extends Adapter {

  // region - Fields for log.
  /** Tag for log. */
  static final String TAG = IMobileMediationAdapter.class.getSimpleName();

  // end region

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_REQUIRES_ACTIVITY_CONTEXT,
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_EMPTY_NATIVE_ADS_LIST
      })

  public @interface AdapterError {

  }

  /**
   * i-mobile adapter error domain.
   */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.imobile";

  /**
   * i-mobile sdk adapter error domain.
   */
  public static final String IMOBILE_SDK_ERROR_DOMAIN = "jp.co.com.google.ads.mediation.imobile";

  /**
   * Activity context is required.
   */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 101;

  /**
   * Server parameters (e.g. publisher ID) are nil.
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 102;

  /**
   * The requested ad size does not match an i-mobile supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 103;

  /**
   * i-mobile's native ad load success callback returned an empty native ads list.
   */
  public static final int ERROR_EMPTY_NATIVE_ADS_LIST = 104;

  private final IMobileSdkWrapper iMobileSdkWrapper;

  IMobileMediationAdapter() {
    iMobileSdkWrapper = new IMobileSdkWrapper();
  }

  @VisibleForTesting
  IMobileMediationAdapter(IMobileSdkWrapper iMobileSdkWrapper) {
    this.iMobileSdkWrapper = iMobileSdkWrapper;
  }

  // region - Adapter interface
  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    // i-mobile does not have any API to retrieve their SDK version.
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = AdapterHelper.getAdapterVersion();
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
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> list) {

    // i-mobile does not have any API for initialization.
    initializationCompleteCallback.onInitializationSucceeded();
  }
  // end region

  // region - Fields for native ads.
  /**
   * Listener for native ads.
   */
  private MediationNativeListener mediationNativeListener;

  // endregion

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    IMobileBannerAd bannerAd = new IMobileBannerAd(callback);
    bannerAd.loadAd(mediationBannerAdConfiguration, iMobileSdkWrapper);
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    IMobileInterstitialAd interstitialAd = new IMobileInterstitialAd(callback, iMobileSdkWrapper);
    interstitialAd.loadAd(mediationInterstitialAdConfiguration);
  }

  @Override
  public void loadNativeAdMapper(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback)
      throws RemoteException {
    IMobileNativeAdLoader nativeAdLoader = new IMobileNativeAdLoader();
    nativeAdLoader.loadAd(
        mediationNativeAdConfiguration, mediationAdLoadCallback, iMobileSdkWrapper);
  }

}