// Copyright 2018 Google LLC
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

package com.applovin.mediation;

import static android.util.Log.DEBUG;
import static android.util.Log.WARN;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link AppLovinBannerAdListener} class is used to forward Banner ad events from the AppLovin
 * SDK to the Google Mobile Ads SDK.
 */
class AppLovinBannerAdListener
    implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener,
    AppLovinAdViewEventListener {

  private final ApplovinAdapter adapter;
  private final MediationBannerListener mediationBannerListener;
  private final AppLovinAdView adView;
  private final String zoneId;

  AppLovinBannerAdListener(
      String zoneId,
      AppLovinAdView adView,
      ApplovinAdapter adapter,
      MediationBannerListener mediationBannerListener) {
    this.adapter = adapter;
    this.mediationBannerListener = mediationBannerListener;
    this.adView = adView;
    this.zoneId = zoneId;
  }

  // Ad Load Listener.
  @Override
  public void adReceived(final AppLovinAd ad) {
    ApplovinAdapter.log(
        DEBUG, "Banner did load ad: " + ad.getAdIdNumber() + " for zone: " + zoneId);
    adView.renderAd(ad);
    AppLovinSdkUtils.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            mediationBannerListener.onAdLoaded(adapter);
          }
        });
  }

  @Override
  public void failedToReceiveAd(final int code) {
    AdError error = AppLovinUtils.getAdError(code);
    ApplovinAdapter.log(WARN, "Failed to load banner ad with error: " + code);
    AppLovinSdkUtils.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            mediationBannerListener.onAdFailedToLoad(adapter, error);
          }
        });
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Banner displayed.");
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Banner dismissed.");
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Banner clicked.");
    mediationBannerListener.onAdClicked(adapter);
  }

  // Ad View Event Listener.
  @Override
  public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    ApplovinAdapter.log(DEBUG, "Banner opened fullscreen.");
    mediationBannerListener.onAdOpened(adapter);
  }

  @Override
  public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    ApplovinAdapter.log(DEBUG, "Banner closed fullscreen.");
    mediationBannerListener.onAdClosed(adapter);
  }

  @Override
  public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView) {
    ApplovinAdapter.log(DEBUG, "Banner left application.");
    mediationBannerListener.onAdLeftApplication(adapter);
  }

  @Override
  public void adFailedToDisplay(
      AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code) {
    ApplovinAdapter.log(WARN, "Banner failed to display: " + code);
  }
}
