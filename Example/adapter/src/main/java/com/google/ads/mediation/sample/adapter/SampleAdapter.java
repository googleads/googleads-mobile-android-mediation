// Copyright 2014 Google LLC
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

package com.google.ads.mediation.sample.adapter;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.google.ads.mediation.sample.sdk.SampleAdRequest;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
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
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A mediation adapter for the Sample ad network. This class can be used as a reference to help
 * other ad networks build their own mediation adapter.
 *
 * <p>NOTE: The audience for this sample is mediation ad networks who are trying to build an ad
 * network adapter, not an app developer trying to integrate Google Mobile Ads into their
 * application.
 *
 * <p>Since the adapter is not directly referenced by the Google Mobile Ads SDK and is instead
 * instantiated with reflection, it's possible that ProGuard might remove it. Use the {@link Keep}}
 * annotation to make sure that the adapter is not removed when minifying the project.
 */
@Keep
public class SampleAdapter extends Adapter {

  protected static final String TAG = SampleAdapter.class.getSimpleName();

  /**
   * Example of an extra field that publishers can use for a Native ad. In this example, the String
   * is added to a {@link Bundle} in {@link SampleNativeAdMapper}.
   */
  public static final String DEGREE_OF_AWESOMENESS = "DegreeOfAwesomeness";

  /**
   * The pixel-to-dpi scale for images downloaded from the sample SDK's URL values. Scale value is
   * set in {@link SampleNativeMappedImage}.
   */
  public static final double SAMPLE_SDK_IMAGE_SCALE = 1.0;

  /**
   * Banner ad renderer for the Sample SDK.
   */
  private SampleBannerAd sampleBannerAd;

  /**
   * Interstitial ad renderer for the Sample SDK.
   */
  private SampleInterstitialAd sampleInterstitialAd;

  /**
   * Rewarded ad renderer for the Sample SDK.
   */
  private SampleRewardedAd sampleRewardedAd;

  /**
   * Native ad renderer for the Sample SDK.
   */
  private SampleNativeAd sampleNativeAd;

  /**
   * Your network probably depends on one or more identifiers that publishers need to provide.
   * Create the keys that your require. For AdMob, only an ad unit ID is required. The key(s) can be
   * whatever you'd prefer. They will be configured on the AdMob front-end later.
   * <p/>
   * Once the AdMob front-end is appropriately configured, the publisher will enter the key/value
   * pair(s) that you require. When your adapter is invoked, you will be provided a {@link Bundle}
   * in each respective `loadAd()` call populated with the expected key/value pair(s) from the
   * server. These value(s) can be obtained using
   * {@link MediationAdConfiguration#getServerParameters()} and should be used to make an ad
   * request.
   */
  public static final String SAMPLE_AD_UNIT_KEY = "ad_unit";

  // region Error Codes
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.sample.adapter";
  public static final String SAMPLE_SDK_ERROR_DOMAIN = "com.google.ads.mediation.sample.sdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      ERROR_INVALID_SERVER_PARAMETERS,
      ERROR_AD_NOT_AVAILABLE,
      ERROR_CONTEXT_NOT_ACTIVITY
  })
  public @interface AdapterError {
  }

  /**
   * Invalid server parameters (e.g. missing placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The ad is not ready to be shown.
   */
  public static final int ERROR_AD_NOT_AVAILABLE = 102;

  /**
   * An {@link android.app.Activity} context is required to show rewarded ads.
   */
  public static final int ERROR_CONTEXT_NOT_ACTIVITY = 103;
  // endregion

  @Override
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {
    initializationCompleteCallback.onInitializationSucceeded();
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
      return new VersionInfo(major, minor, micro);
    }

    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = SampleAdRequest.getSDKVersion();
    String[] splits = versionString.split("\\.");

    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    return new VersionInfo(0, 0, 0);
  }

  /**
   * This method will only ever be called once per adapter instance.
   */
  @Override
  public void loadBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    sampleBannerAd = new SampleBannerAd(mediationBannerAdConfiguration, callback);
    sampleBannerAd.loadAd();
  }

  /**
   * This method will only ever be called once per adapter instance.
   */
  @Override
  public void loadInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    sampleInterstitialAd = new SampleInterstitialAd(mediationInterstitialAdConfiguration, callback);
    sampleInterstitialAd.loadAd();
  }

  /**
   * This method will only ever be called once per adapter instance.
   */
  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    sampleRewardedAd = new SampleRewardedAd(mediationRewardedAdConfiguration, callback);
    sampleRewardedAd.loadAd();
  }

  /**
   * This method will only ever be called once per adapter instance.
   */
  @Override
  public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    sampleNativeAd = new SampleNativeAd(mediationNativeAdConfiguration, callback);
    sampleNativeAd.loadAd();
  }

  /**
   * The {@link MediationExtrasBundleBuilder} class is used to create a mediation extras bundle that
   * can be passed to the adapter as extra data to be used in making requests. In this example the
   * sample SDK has two extra parameters that it can use to customize its ad requests.
   */
  public static final class MediationExtrasBundleBuilder {

    // Keys to add and obtain the extra parameters from the bundle.
    public static final String KEY_AWESOME_SAUCE = "awesome_sauce";
    public static final String KEY_INCOME = "income";

    /**
     * An extra value used to populate the "ShouldAddAwesomeSauce" property of the Sample SDK's ad
     * request.
     */
    private boolean shouldAddAwesomeSauce;

    /**
     * An extra value used to populate the "income" property of the Sample SDK's ad request.
     */
    private int income;

    public MediationExtrasBundleBuilder setShouldAddAwesomeSauce(
        boolean shouldAddAwesomeSauce) {
      this.shouldAddAwesomeSauce = shouldAddAwesomeSauce;
      return MediationExtrasBundleBuilder.this;
    }

    public MediationExtrasBundleBuilder setIncome(int income) {
      this.income = income;
      return MediationExtrasBundleBuilder.this;
    }

    public Bundle build() {
      Bundle extras = new Bundle();
      extras.putBoolean(KEY_AWESOME_SAUCE, shouldAddAwesomeSauce);
      extras.putInt(KEY_INCOME, income);
      return extras;
    }
  }
}
