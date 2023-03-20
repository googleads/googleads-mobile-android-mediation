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

package com.jirbo.adcolony;

import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.TAG;
import static com.google.ads.mediation.adcolony.AdColonyMediationAdapter.createSdkError;

import android.util.Log;
import androidx.annotation.NonNull;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link AdColonyBannerAdListener} class is used to forward Banner ad events from AdColony SDK
 * to Google Mobile Ads SDK.
 */
class AdColonyBannerAdListener extends AdColonyAdViewListener {

  /**
   * The MediationBannerListener used to report callbacks.
   */
  private MediationBannerListener mediationBannerListener;
  /**
   * The AdColony banner adapter.
   */
  private AdColonyAdapter adapter;

  AdColonyBannerAdListener(@NonNull AdColonyAdapter adapter,
      @NonNull MediationBannerListener listener) {
    this.mediationBannerListener = listener;
    this.adapter = adapter;
  }

  @Override
  public void onClicked(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdClicked(adapter);
    }
  }

  @Override
  public void onOpened(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdOpened(adapter);
    }
  }

  @Override
  public void onClosed(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdClosed(adapter);
    }
  }

  @Override
  public void onLeftApplication(AdColonyAdView ad) {
    if (mediationBannerListener != null && adapter != null) {
      mediationBannerListener.onAdLeftApplication(adapter);
    }
  }

  @Override
  public void onRequestFilled(AdColonyAdView adColonyAdView) {
    if (mediationBannerListener != null && adapter != null) {
      adapter.setAdView(adColonyAdView);
      mediationBannerListener.onAdLoaded(adapter);
    }
  }

  @Override
  public void onRequestNotFilled(AdColonyZone zone) {
    if (mediationBannerListener != null && adapter != null) {
      AdError error = createSdkError();
      Log.w(TAG, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(adapter, error);
    }
  }

  void destroy() {
    adapter = null;
    mediationBannerListener = null;
  }
}
