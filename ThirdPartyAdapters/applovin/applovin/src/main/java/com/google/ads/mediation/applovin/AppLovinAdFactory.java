// Copyright 2023 Google LLC
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

package com.google.ads.mediation.applovin;

import android.content.Context;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdSize;

/** A factory to create {@link AppLovinAdView} for AppLovin ads. */
public class AppLovinAdFactory {
  AppLovinAdViewWrapper createAdView(
      AppLovinSdk sdk, AppLovinAdSize appLovinAdSize, AdSize adSize, Context context) {
    return AppLovinAdViewWrapper.newInstance(sdk, appLovinAdSize, adSize, context);
  }

  public AppLovinInterstitialAdDialog createInterstitialAdDialog(AppLovinSdk sdk, Context context) {
    return AppLovinInterstitialAd.create(sdk, context);
  }

  public AppLovinIncentivizedInterstitial createIncentivizedInterstitial(AppLovinSdk sdk) {
    return AppLovinIncentivizedInterstitial.create(sdk);
  }

  public AppLovinIncentivizedInterstitial createIncentivizedInterstitial(
      String zoneId, AppLovinSdk sdk) {
    return AppLovinIncentivizedInterstitial.create(zoneId, sdk);
  }
}
