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

package com.google.ads.mediation.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.jirbo.adcolony.AdColonyManager;
import java.util.ArrayList;

public class AdColonyInterstitialRenderer extends AdColonyInterstitialListener implements
    MediationInterstitialAd {

  private MediationInterstitialAdCallback interstitialAdCallback;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;
  private AdColonyInterstitial adColonyInterstitial;
  private final MediationInterstitialAdConfiguration adConfiguration;

  AdColonyInterstitialRenderer(@NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    this.adLoadCallback = callback;
    this.adConfiguration = adConfiguration;
  }

  public void render() {
    AdColony.setAppOptions(AdColonyManager.getInstance().buildAppOptions(adConfiguration));
    AdColonyAdOptions adOptions = AdColonyManager.getInstance()
        .getAdOptionsFromAdConfig(adConfiguration);
    ArrayList<String> listFromServerParams = AdColonyManager.getInstance()
        .parseZoneList(adConfiguration.getServerParameters());
    String requestedZone = AdColonyManager.getInstance()
        .getZoneFromRequest(listFromServerParams, adConfiguration.getMediationExtras());
    AdColony.requestInterstitial(requestedZone, this, adOptions);
  }

  @Override
  public void showAd(@NonNull Context context) {
    adColonyInterstitial.show();
  }

  @Override
  public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
    AdColonyInterstitialRenderer.this.adColonyInterstitial = adColonyInterstitial;
    interstitialAdCallback = adLoadCallback.onSuccess(AdColonyInterstitialRenderer.this);
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    AdError error = createSdkError();
    Log.w(TAG, error.getMessage());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onLeftApplication(AdColonyInterstitial ad) {
    super.onLeftApplication(ad);

    interstitialAdCallback.reportAdClicked();
    interstitialAdCallback.onAdLeftApplication();
  }

  @Override
  public void onOpened(AdColonyInterstitial ad) {
    super.onOpened(ad);

    interstitialAdCallback.onAdOpened();
    interstitialAdCallback.reportAdImpression();
  }

  @Override
  public void onClosed(AdColonyInterstitial ad) {
    super.onClosed(ad);

    interstitialAdCallback.onAdClosed();
  }

  @Override
  public void onExpiring(AdColonyInterstitial ad) {
    super.onExpiring(ad);

    AdColony.requestInterstitial(ad.getZoneID(), this);
  }
}
