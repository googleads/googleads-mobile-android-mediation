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

import static android.util.Log.DEBUG;
import static android.util.Log.WARN;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * Created by Thomas So on July 17 2018
 */
public final class AppLovinRtbBannerRenderer
    implements MediationBannerAd,
    AppLovinAdLoadListener,
    AppLovinAdDisplayListener,
    AppLovinAdClickListener,
    AppLovinAdViewEventListener {

  private static final String TAG = AppLovinRtbBannerRenderer.class.getSimpleName();

  /**
   * Data used to render an RTB banner ad.
   */
  private final MediationBannerAdConfiguration adConfiguration;

  /**
   * Callback object to notify the Google Mobile Ads SDK if ad rendering succeeded or failed.
   */
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;

  /**
   * Listener object to notify the Google Mobile Ads SDK of banner presentation events.
   */
  private MediationBannerAdCallback bannerAdCallback;

  private AppLovinAdView adView;

  public AppLovinRtbBannerRenderer(
      MediationBannerAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
    this.adConfiguration = adConfiguration;
    this.callback = callback;
  }

  public void loadAd() {
    Context context = adConfiguration.getContext();

    AppLovinAdSize appLovinAdSize = AppLovinAdSize.BANNER;
    AdSize googleAdSize = adConfiguration.getAdSize();
    if (googleAdSize.getWidth() >= 300 && googleAdSize.getHeight() >= 250) {
      appLovinAdSize = AppLovinAdSize.MREC;
    } else if (googleAdSize.getWidth() >= 728 && googleAdSize.getHeight() >= 90) {
      appLovinAdSize = AppLovinAdSize.LEADER;
    } else if (googleAdSize.getWidth() >= 320 && googleAdSize.getHeight() >= 50) {
      appLovinAdSize = AppLovinAdSize.BANNER;
    }

    AppLovinSdk sdk = AppLovinUtils.retrieveSdk(adConfiguration.getServerParameters(), context);
    adView = new AppLovinAdView(sdk, appLovinAdSize, context);
    adView.setAdDisplayListener(this);
    adView.setAdClickListener(this);
    adView.setAdViewEventListener(this);

    // Load ad!
    sdk.getAdService().loadNextAdForAdToken(adConfiguration.getBidResponse(), this);
  }

  @NonNull
  @Override
  public View getView() {
    return adView;
  }

  // region AppLovin Listeners
  @Override
  public void adReceived(AppLovinAd ad) {
    Log.d(TAG, "Banner did load ad: " + ad.getAdIdNumber());

    bannerAdCallback = callback.onSuccess(AppLovinRtbBannerRenderer.this);
    adView.renderAd(ad);
  }

  @Override
  public void failedToReceiveAd(int code) {
    AdError error = AppLovinUtils.getAdError(code);
    log(DEBUG, error.getMessage());
    callback.onFailure(error);
  }

  @Override
  public void adDisplayed(AppLovinAd ad) {
    Log.d(TAG, "Banner displayed.");
    bannerAdCallback.reportAdImpression();
    bannerAdCallback.onAdOpened();
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    Log.d(TAG, "Banner hidden.");
  }

  @Override
  public void adClicked(AppLovinAd ad) {
    Log.d(TAG, "Banner clicked.");
    bannerAdCallback.reportAdClicked();
  }

  @Override
  public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner opened fullscreen.");
    bannerAdCallback.onAdOpened();
  }

  @Override
  public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner closed fullscreen.");
    bannerAdCallback.onAdClosed();
  }

  @Override
  public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner left application.");
    bannerAdCallback.onAdLeftApplication();
  }

  @Override
  public void adFailedToDisplay(
      AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code) {
    log(WARN, "Banner failed to display: " + code);
  }
  // endregion

}
