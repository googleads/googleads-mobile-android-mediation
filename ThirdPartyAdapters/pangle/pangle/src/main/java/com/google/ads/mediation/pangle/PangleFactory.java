// Copyright 2022 Google LLC
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

import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import com.google.ads.mediation.pangle.renderer.PangleAppOpenAd;
import com.google.ads.mediation.pangle.renderer.PangleBannerAd;
import com.google.ads.mediation.pangle.renderer.PangleInterstitialAd;
import com.google.ads.mediation.pangle.renderer.PangleNativeAd;
import com.google.ads.mediation.pangle.renderer.PangleRewardedAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

/**
 * A factory for creating objects for Pangle adapter.
 *
 * <p>This factory exists to make it possible to replace the real objects with mock objects during
 * unit testing.
 */
public class PangleFactory {

  PAGConfig.Builder createPAGConfigBuilder() {
    return new PAGConfig.Builder();
  }

  public PAGAppOpenRequest createPagAppOpenRequest() {
    return new PAGAppOpenRequest();
  }

  public PAGBannerRequest createPagBannerRequest(PAGBannerSize pagBannerSize) {
    return new PAGBannerRequest(pagBannerSize);
  }

  public PAGInterstitialRequest createPagInterstitialRequest() {
    return new PAGInterstitialRequest();
  }

  public PAGNativeRequest createPagNativeRequest() {
    return new PAGNativeRequest();
  }

  public PAGRewardedRequest createPagRewardedRequest() {
    return new PAGRewardedRequest();
  }

  PangleAppOpenAd createPangleAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleSdkWrapper pangleSdkWrapper
     ) {
    return new PangleAppOpenAd(
        mediationAppOpenAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this
        );
  }

  PangleBannerAd createPangleBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleSdkWrapper pangleSdkWrapper) {
    return new PangleBannerAd(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this);
  }

  PangleInterstitialAd createPangleInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      PangleSdkWrapper pangleSdkWrapper) {
    return new PangleInterstitialAd(
        mediationInterstitialAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this);
  }

  PangleNativeAd createPangleNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      PangleSdkWrapper pangleSdkWrapper) {
    return new PangleNativeAd(
        mediationNativeAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this);
  }

  PangleRewardedAd createPangleRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      PangleSdkWrapper pangleSdkWrapper) {
    return new PangleRewardedAd(
        mediationRewardedAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this);
  }
}
