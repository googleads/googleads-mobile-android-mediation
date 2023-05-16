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

package com.applovin.mediation.rtb;

import android.content.Context;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.google.ads.mediation.applovin.AppLovinAdFactory;
import com.google.ads.mediation.applovin.AppLovinInitializer;
import com.google.ads.mediation.applovin.AppLovinRewardedRenderer;
import com.google.ads.mediation.applovin.AppLovinSdkUtilsWrapper;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public final class AppLovinRtbRewardedRenderer extends AppLovinRewardedRenderer {

  /**
   * AppLovin rewarded ad object.
   */
  private AppLovinAd appLovinAd;

  public AppLovinRtbRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory,
      @NonNull AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper) {
    super(
        adConfiguration, callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
  }

  @Override
  public void loadAd() {
    Context context = adConfiguration.getContext();
    appLovinSdk =
        AppLovinInitializer.getInstance()
            .retrieveSdk(adConfiguration.getServerParameters(), context);

    // Create rewarded video object.
    incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(appLovinSdk);
    incentivizedInterstitial.setExtraInfo("google_watermark", adConfiguration.getWatermark());

    // Load ad.
    appLovinSdk.getAdService().loadNextAdForAdToken(
        adConfiguration.getBidResponse(), AppLovinRtbRewardedRenderer.this);
  }

  @Override
  public void showAd(@NonNull Context context) {
    appLovinSdk.getSettings()
        .setMuted(AppLovinUtils.shouldMuteAudio(adConfiguration.getMediationExtras()));

    incentivizedInterstitial.show(AppLovinRtbRewardedRenderer.this.appLovinAd,
        context, AppLovinRtbRewardedRenderer.this, AppLovinRtbRewardedRenderer.this,
        AppLovinRtbRewardedRenderer.this, AppLovinRtbRewardedRenderer.this);
  }

  // region AppLovinAdLoadListener implementation
  @Override
  public void adReceived(@NonNull AppLovinAd appLovinAd) {
    AppLovinRtbRewardedRenderer.this.appLovinAd = appLovinAd;
    super.adReceived(AppLovinRtbRewardedRenderer.this.appLovinAd);
  }
  // endregion
}
