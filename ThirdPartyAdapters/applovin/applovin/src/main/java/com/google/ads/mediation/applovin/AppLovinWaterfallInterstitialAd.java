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

import static com.applovin.sdk.AppLovinAdSize.INTERSTITIAL;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_MISSING_SDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinSdk;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Used to load AppLovin interstitial ads and mediate callbacks between Google Mobile Ads SDK and
 * AppLovin SDK
 */
public class AppLovinWaterfallInterstitialAd extends AppLovinInterstitialRenderer
    implements MediationInterstitialAd {

  @VisibleForTesting
  protected static final HashMap<String, WeakReference<AppLovinWaterfallInterstitialAd>>
      appLovinWaterfallInterstitialAds = new HashMap<>();

  private AppLovinSdk sdk;

  private Context context;

  private Bundle networkExtras;

  // Flag to let multiple loading of ads
  private boolean enableMultipleAdLoading = false;

  public AppLovinWaterfallInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory) {
    super(adConfiguration, callback, appLovinInitializer, appLovinAdFactory);
  }

  @Override
  public void loadAd() {
    context = interstitialAdConfiguration.getContext();
    Bundle serverParameters = interstitialAdConfiguration.getServerParameters();
    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, ERROR_MSG_MISSING_SDK, APPLOVIN_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      interstitialAdLoadCallback.onFailure(error);
      return;
    }

    if (AppLovinUtils.isMultiAdsEnabled()) {
      enableMultipleAdLoading = true;
    }

    appLovinInitializer.initialize(
        context,
        sdkKey,
        new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess(@NonNull String sdkKey) {
            zoneId = AppLovinUtils.retrieveZoneId(serverParameters);
            if (appLovinWaterfallInterstitialAds.containsKey(zoneId)
                && appLovinWaterfallInterstitialAds.get(zoneId).get() != null) {
              AdError error =
                  new AdError(
                      ERROR_AD_ALREADY_REQUESTED, ERROR_MSG_MULTIPLE_INTERSTITIAL_AD, ERROR_DOMAIN);
              Log.e(TAG, error.getMessage());
              interstitialAdLoadCallback.onFailure(error);
              return;
            }
            appLovinWaterfallInterstitialAds.put(
                zoneId, new WeakReference<>(AppLovinWaterfallInterstitialAd.this));

            // Store parent objects.
            sdk = appLovinInitializer.retrieveSdk(serverParameters, context);
            AppLovinWaterfallInterstitialAd.this.networkExtras = networkExtras;

            Log.d(TAG, "Requesting interstitial for zone: " + zoneId);

            if (!TextUtils.isEmpty(zoneId)) {
              sdk.getAdService().loadNextAdForZoneId(zoneId, AppLovinWaterfallInterstitialAd.this);
            } else {
              sdk.getAdService().loadNextAd(INTERSTITIAL, AppLovinWaterfallInterstitialAd.this);
            }
          }
        });
  }

  @Override
  public void showAd(Context context) {
    // Update mute state.
    sdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(networkExtras));

    final AppLovinInterstitialAdDialog interstitialAdDialog =
        appLovinAdFactory.createInterstitialAdDialog(sdk, context);

    interstitialAdDialog.setAdDisplayListener(this);
    interstitialAdDialog.setAdClickListener(this);
    interstitialAdDialog.setAdVideoPlaybackListener(this);

    if (appLovinInterstitialAd == null) {
      Log.d(TAG, "Attempting to show interstitial before one was loaded.");

      // Check if we have a default zone interstitial available.
      if (TextUtils.isEmpty(zoneId)) {
        Log.d(TAG, "Showing interstitial preloaded by SDK.");
        interstitialAdDialog.show();
      } else {
        // TODO: Show ad for zone identifier if exists
      }
      return;
    }

    Log.d(TAG, "Showing interstitial for zone: " + zoneId);
    interstitialAdDialog.showAndRender(appLovinInterstitialAd);
  }

  @Override
  public void adReceived(AppLovinAd ad) {
    if (enableMultipleAdLoading) {
      unregister();
    }
    super.adReceived(ad);
  }

  @Override
  public void failedToReceiveAd(final int code) {
    unregister();
    super.failedToReceiveAd(code);
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    unregister();
    super.adHidden(ad);
  }

  void unregister() {
    if (!TextUtils.isEmpty(zoneId)
        && appLovinWaterfallInterstitialAds.containsKey(zoneId)
        && this.equals(appLovinWaterfallInterstitialAds.get(zoneId).get())) {
      appLovinWaterfallInterstitialAds.remove(zoneId);
    }
  }
}
