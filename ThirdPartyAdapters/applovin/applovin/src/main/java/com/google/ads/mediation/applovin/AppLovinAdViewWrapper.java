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
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;

/**
 * Wrapper class for an instance {@link AppLovinAdView} created by {@link AppLovinAdViewFactory}. It
 * is used as a layer between the Adapter's and the AppLovin SDK to facilitate unit testing.
 */
class AppLovinAdViewWrapper {

  private final AppLovinAdView appLovinAdView;

  private AppLovinAdViewWrapper(AppLovinSdk sdk, AppLovinAdSize appLovinAdSize, Context context) {
    appLovinAdView = new AppLovinAdView(sdk, appLovinAdSize, context);
  }

  public static AppLovinAdViewWrapper newInstance(
      AppLovinSdk sdk, AppLovinAdSize appLovinAdSize, Context context) {
    return new AppLovinAdViewWrapper(sdk, appLovinAdSize, context);
  }

  public void setAdDisplayListener(AppLovinAdDisplayListener adDisplayListener) {
    appLovinAdView.setAdDisplayListener(adDisplayListener);
  }

  public void setAdClickListener(AppLovinAdClickListener adClickListener) {
    appLovinAdView.setAdClickListener(adClickListener);
  }

  public void setAdViewEventListener(AppLovinAdViewEventListener adViewEventListener) {
    appLovinAdView.setAdViewEventListener(adViewEventListener);
  }

  public void renderAd(AppLovinAd ad) {
    appLovinAdView.renderAd(ad);
  }

  public AppLovinAdView getAppLovinAdView() {
    return appLovinAdView;
  }
}
