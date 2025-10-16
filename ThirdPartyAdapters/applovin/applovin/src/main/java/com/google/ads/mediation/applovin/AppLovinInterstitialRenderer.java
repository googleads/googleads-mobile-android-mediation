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

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/**
 * Superclass to be extended by classes that implement AppLovin Interstitial Ads using either
 * Waterfall or RTB. This class serves as bridge between AppLovinSdk and GMA SDK ad interactions.
 * Subclasses must implement loadAd and showAd methods.
 */
public abstract class AppLovinInterstitialRenderer
    implements MediationInterstitialAd,
        AppLovinAdDisplayListener,
        AppLovinAdClickListener,
        AppLovinAdVideoPlaybackListener,
        AppLovinAdLoadListener {

  protected static final String TAG = AppLovinInterstitialRenderer.class.getSimpleName();

  @VisibleForTesting
  public static final String ERROR_MSG_MULTIPLE_INTERSTITIAL_AD =
      " Cannot load multiple interstitial ads with the same Zone ID. Let the first ad finish"
          + " loading before attempting to load another. ";

  protected final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      interstitialAdLoadCallback;

  protected final AppLovinInitializer appLovinInitializer;

  protected final AppLovinAdFactory appLovinAdFactory;

  @Nullable private MediationInterstitialAdCallback interstitialAdCallback;

  @Nullable protected AppLovinAd appLovinInterstitialAd;

  @Nullable protected String zoneId;

  @Nullable protected Bundle networkExtras;

  public AppLovinInterstitialRenderer(
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory) {
    interstitialAdLoadCallback = callback;
    this.appLovinInitializer = appLovinInitializer;
    this.appLovinAdFactory = appLovinAdFactory;
  }

  public abstract void loadAd(@NonNull MediationInterstitialAdConfiguration interstitialAdConfiguration);

  @Override
  public void adReceived(final AppLovinAd ad) {
    Log.d(TAG, "Interstitial did load ad for zone: " + zoneId);
    appLovinInterstitialAd = ad;

    interstitialAdCallback = interstitialAdLoadCallback.onSuccess(this);
  }

  @Override
  public void failedToReceiveAd(final int code) {
    AdError error = AppLovinUtils.getAdError(code);
    Log.w(TAG, error.getMessage());
    interstitialAdLoadCallback.onFailure(error);
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    Log.d(TAG, "Interstitial displayed.");
    interstitialAdCallback.onAdOpened();
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    Log.d(TAG, "Interstitial dismissed.");
    interstitialAdCallback.onAdClosed();
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    Log.d(TAG, "Interstitial clicked.");
    interstitialAdCallback.reportAdClicked();
    interstitialAdCallback.onAdLeftApplication();
  }

  // Ad Video Playback Listener.
  @Override
  public void videoPlaybackBegan(AppLovinAd ad) {
    Log.d(TAG, "Interstitial video playback began.");
  }

  @Override
  public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
    Log.d(TAG, "Interstitial video playback ended at playback percent: " + percentViewed + "%.");
  }
}
