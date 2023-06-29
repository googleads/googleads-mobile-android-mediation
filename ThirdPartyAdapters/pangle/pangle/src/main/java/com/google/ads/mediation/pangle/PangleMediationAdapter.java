// Copyright 2021 Google LLC
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

package com.google.ads.mediation.pangle;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGDoNotSellType;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGGDPRConsentType;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.ads.mediation.pangle.renderer.PangleAppOpenAd;
import com.google.ads.mediation.pangle.renderer.PangleBannerAd;
import com.google.ads.mediation.pangle.renderer.PangleInterstitialAd;
import com.google.ads.mediation.pangle.renderer.PangleNativeAd;
import com.google.ads.mediation.pangle.renderer.PangleRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import java.util.HashSet;
import java.util.List;

public class PangleMediationAdapter extends RtbAdapter {

  public static final String TAG = PangleMediationAdapter.class.getSimpleName();

  @VisibleForTesting
  static final String ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID = "Missing or invalid App ID.";

  private final PangleInitializer pangleInitializer;
  private final PangleSdkWrapper pangleSdkWrapper;
  private final PangleFactory pangleFactory;
  private final PanglePrivacyConfig panglePrivacyConfig;

  private PangleAppOpenAd appOpenAd;
  private PangleBannerAd bannerAd;
  private PangleInterstitialAd interstitialAd;
  private PangleNativeAd nativeAd;
  private PangleRewardedAd rewardedAd;

  private static int gdpr = -1;
  private static int ccpa = -1;

  PangleMediationAdapter() {
    pangleInitializer = PangleInitializer.getInstance();
    pangleSdkWrapper = new PangleSdkWrapper();
    pangleFactory = new PangleFactory();
    panglePrivacyConfig = new PanglePrivacyConfig(pangleSdkWrapper);
  }

  @VisibleForTesting
  PangleMediationAdapter(
      PangleInitializer pangleInitializer,
      PangleSdkWrapper pangleSdkWrapper,
      PangleFactory pangleFactory,
      PanglePrivacyConfig panglePrivacyConfig) {
    this.pangleInitializer = pangleInitializer;
    this.pangleSdkWrapper = pangleSdkWrapper;
    this.pangleFactory = pangleFactory;
    this.panglePrivacyConfig = panglePrivacyConfig;
  }

  @Override
  public void collectSignals(
      @NonNull RtbSignalData rtbSignalData, @NonNull SignalCallbacks signalCallbacks) {
    // The user data needs to be set for it to be included in the signals.
    Bundle networkExtras = rtbSignalData.getNetworkExtras();
    if (networkExtras != null && networkExtras.containsKey(PangleExtras.Keys.USER_DATA)) {
      pangleSdkWrapper.setUserData(networkExtras.getString(PangleExtras.Keys.USER_DATA, ""));
    }
    String biddingToken = pangleSdkWrapper.getBiddingToken();
    signalCallbacks.onSuccess(biddingToken);
  }

  @Override
  public void initialize(
      @NonNull Context context,
      @NonNull final InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> list) {
    HashSet<String> appIds = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : list) {
      Bundle serverParameters = mediationConfiguration.getServerParameters();
      String appId = serverParameters.getString(PangleConstants.APP_ID);
      if (!TextUtils.isEmpty(appId)) {
        appIds.add(appId);
      }
    }

    int count = appIds.size();
    if (count <= 0) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, ERROR_MESSAGE_MISSING_OR_INVALID_APP_ID);
      Log.w(TAG, error.toString());
      initializationCompleteCallback.onInitializationFailed(error.toString());
      return;
    }

    String appId = appIds.iterator().next();
    if (count > 1) {
      String message =
          String.format(
              "Found multiple app IDs in %s. Using %s to initialize Pangle SDK.", appIds, appId);
      Log.w(TAG, message);
    }

    panglePrivacyConfig.setCoppa(
        MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
    pangleInitializer.initialize(
        context,
        appId,
        new Listener() {
          @Override
          public void onInitializeSuccess() {
            initializationCompleteCallback.onInitializationSucceeded();
          }

          @Override
          public void onInitializeError(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            initializationCompleteCallback.onInitializationFailed(error.getMessage());
          }
        });
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

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = PAGSdk.getSDKVersion();
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
  public void loadAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    appOpenAd =
        pangleFactory.createPangleAppOpenAd(
            adConfiguration, callback, pangleInitializer, pangleSdkWrapper, panglePrivacyConfig);
    appOpenAd.render();
  }

  @Override
  public void loadBannerAd(
      @NonNull MediationBannerAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    bannerAd =
        pangleFactory.createPangleBannerAd(
            adConfiguration, callback, pangleInitializer, pangleSdkWrapper, panglePrivacyConfig);
    bannerAd.render();
  }

  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback) {
    interstitialAd =
        pangleFactory.createPangleInterstitialAd(
            adConfiguration, callback, pangleInitializer, pangleSdkWrapper, panglePrivacyConfig);
    interstitialAd.render();
  }

  @Override
  public void loadNativeAd(
      @NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    nativeAd =
        new PangleNativeAd(adConfiguration, callback, pangleInitializer, panglePrivacyConfig);
    nativeAd.render();
  }

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    rewardedAd =
        pangleFactory.createPangleRewardedAd(
            adConfiguration, callback, pangleInitializer, pangleSdkWrapper, panglePrivacyConfig);
    rewardedAd.render();
  }

  /**
   * Set the GDPR setting in Pangle SDK.
   *
   * @param gdpr an {@code Integer} value that indicates whether the user consents the use of
   *     personal data to serve ads under GDPR. See <a
   *     href="https://www.pangleglobal.com/integration/android-initialize-pangle-sdk">Pangle's
   *     documentation</a> for more information about what values may be provided.
   */
  public static void setGDPRConsent(@PAGGDPRConsentType int gdpr) {
    setGDPRConsent(gdpr, new PangleSdkWrapper());
  }

  @VisibleForTesting
  static void setGDPRConsent(@PAGGDPRConsentType int gdpr, PangleSdkWrapper pangleSdkWrapper) {
    if (gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_CONSENT
        && gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_NO_CONSENT
        && gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT) {
      // no-op
      Log.w(TAG, "Invalid GDPR value. Pangle SDK only accepts -1, 0 or 1.");
      return;
    }
    if (pangleSdkWrapper.isInitSuccess()) {
      pangleSdkWrapper.setGdprConsent(gdpr);
    }
    PangleMediationAdapter.gdpr = gdpr;
  }

  public static int getGDPRConsent() {
    return gdpr;
  }

  /**
   * Set the CCPA setting in Pangle SDK.
   *
   * @param ccpa an {@code Integer} value that indicates whether the user opts in of the "sale" of
   *     the "personal information" under CCPA. See <a
   *     href="https://www.pangleglobal.com/integration/android-initialize-pangle-sdk">Pangle's
   *     documentation</a> for more information about what values may be provided.
   */
  public static void setDoNotSell(@PAGDoNotSellType int ccpa) {
    setDoNotSell(ccpa, new PangleSdkWrapper());
  }

  @VisibleForTesting
  static void setDoNotSell(@PAGDoNotSellType int ccpa, PangleSdkWrapper pangleSdkWrapper) {
    if (ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_SELL
        && ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_NOT_SELL
        && ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT) {
      // no-op
      Log.w(TAG, "Invalid CCPA value. Pangle SDK only accepts -1, 0 or 1.");
      return;
    }
    if (pangleSdkWrapper.isInitSuccess()) {
      pangleSdkWrapper.setDoNotSell(ccpa);
    }
    PangleMediationAdapter.ccpa = ccpa;
  }

  public static int getDoNotSell() {
    return ccpa;
  }
}
