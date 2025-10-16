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

package com.applovin.mediation.rtb;

import static com.applovin.mediation.AppLovinExtras.Keys.KEY_WATERMARK;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinSdk;
import com.google.ads.mediation.applovin.AppLovinAdFactory;
import com.google.ads.mediation.applovin.AppLovinInitializer;
import com.google.ads.mediation.applovin.AppLovinInterstitialRenderer;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/** Created by Thomas So on July 17 2018 */
public final class AppLovinRtbInterstitialRenderer extends AppLovinInterstitialRenderer
    implements MediationInterstitialAd {

  private AppLovinSdk sdk;

  @Nullable private AppLovinInterstitialAdDialog interstitialAd;

  public AppLovinRtbInterstitialRenderer(
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory) {
    super(callback, appLovinInitializer, appLovinAdFactory);
  }

  @Override
  public void loadAd(@NonNull MediationInterstitialAdConfiguration interstitialAdConfiguration) {
    sdk = appLovinInitializer.retrieveSdk(interstitialAdConfiguration.getContext());
    // Create interstitial object
    interstitialAd =
        appLovinAdFactory.createInterstitialAdDialog(sdk, interstitialAdConfiguration.getContext());
    interstitialAd.setAdDisplayListener(this);
    interstitialAd.setAdClickListener(this);
    interstitialAd.setAdVideoPlaybackListener(this);
    interstitialAd.setExtraInfo(KEY_WATERMARK, interstitialAdConfiguration.getWatermark());
    networkExtras = interstitialAdConfiguration.getMediationExtras();

    // Load ad!
    sdk.getAdService().loadNextAdForAdToken(interstitialAdConfiguration.getBidResponse(), this);
  }

  @Override
  public void showAd(@NonNull Context context) {
    // Update mute state
    boolean muted = AppLovinUtils.shouldMuteAudio(networkExtras);
    sdk.getSettings().setMuted(muted);

    interstitialAd.showAndRender(appLovinInterstitialAd);
  }
}
