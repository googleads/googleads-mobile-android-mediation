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

package com.google.android.gms.ads.mediation;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.google.android.gms.ads.AdSize;

public class ZucksAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {

  // region Lifecycle methods
  // TODO: Fill with any required lifecycle events.
  @Override
  public void onPause() {

  }

  @Override
  public void onResume() {

  }

  @Override
  public void onDestroy() {

  }
  // endregion

  // region MediationBannerAdapter methods
  @Override
  public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener,
      Bundle serverParameters, AdSize adSize, MediationAdRequest mediationAdRequest,
      Bundle mediationExtras) {
    // TODO: Load banner ad and forward the success callback:
    mediationBannerListener.onAdLoaded(ZucksAdapter.this);
  }

  @Override
  public View getBannerView() {
    // TODO: Return the Zucks banner ad View object.
    return null;
  }
  // endregion

  // region MediationInterstitialAdapter methods
  @Override
  public void requestInterstitialAd(Context context,
      MediationInterstitialListener mediationInterstitialListener, Bundle serverParameters,
      MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
    // TODO: Load interstitial ad and forward the success callback:
    mediationInterstitialListener.onAdLoaded(ZucksAdapter.this);
  }

  @Override
  public void showInterstitial() {
    // TODO: Show interstitial ad.
  }
  // endregion
}
