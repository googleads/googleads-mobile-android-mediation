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

package com.google.ads.mediation.adcolony;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyInterstitialListener;

public class AdColonyWrapper {

  @SuppressWarnings("NonFinalStaticField")
  private static AdColonyWrapper instance = null;

  public static AdColonyWrapper getInstance() {
    if (instance == null) {
      instance = new AdColonyWrapper();
    }
    return instance;
  }

  public void requestInterstitial(
      String requestedZone, AdColonyInterstitialListener listener, AdColonyAdOptions adOptions) {
    AdColony.requestInterstitial(requestedZone, listener, adOptions);
  }

  public void requestAdView(
      String requestedZone,
      AdColonyAdViewListener listener,
      AdColonyAdSize adSize,
      AdColonyAdOptions adOptions) {
    AdColony.requestAdView(requestedZone, listener, adSize, adOptions);
  }
}
