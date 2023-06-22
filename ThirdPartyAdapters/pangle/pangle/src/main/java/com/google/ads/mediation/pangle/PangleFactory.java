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
import com.google.ads.mediation.pangle.renderer.PangleBannerAd;
import com.google.ads.mediation.pangle.renderer.PangleInterstitialAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

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

  public PAGBannerRequest createPagBannerRequest(PAGBannerSize pagBannerSize) {
    return new PAGBannerRequest(pagBannerSize);
  }

  public PAGInterstitialRequest createPagInterstitialRequest() {
    return new PAGInterstitialRequest();
  }

  PangleBannerAd createPangleBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleSdkWrapper pangleSdkWrapper,
      @NonNull PanglePrivacyConfig panglePrivacyConfig) {
    return new PangleBannerAd(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this,
        panglePrivacyConfig);
  }

  PangleInterstitialAd createPangleInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      PangleSdkWrapper pangleSdkWrapper,
      @NonNull PanglePrivacyConfig panglePrivacyConfig) {
    return new PangleInterstitialAd(
        mediationInterstitialAdConfiguration,
        mediationAdLoadCallback,
        pangleInitializer,
        pangleSdkWrapper,
        this,
        panglePrivacyConfig);
  }
}
