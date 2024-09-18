// Copyright 2017 Google LLC
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
import static android.util.Log.ERROR;
import static android.util.Log.WARN;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.ads.mediation.applovin.AppLovinInitializer;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * The {@link ApplovinAdapter} class is used to load AppLovin Banner, interstitial & rewarded-based
 * video ads and to mediate the callbacks between the AppLovin SDK and the Google Mobile Ads SDK.
 */
public class ApplovinAdapter extends AppLovinMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter, OnContextChangedListener {

  private static final boolean LOGGING_ENABLED = true;

  // Interstitial globals.
  private static final HashMap<String, WeakReference<ApplovinAdapter>> appLovinInterstitialAds =
      new HashMap<>();
  private AppLovinAd appLovinInterstitialAd;

  // Parent objects.
  private AppLovinSdk sdk;
  private Context context;
  private Bundle networkExtras;

  // Interstitial objects.
  private MediationInterstitialListener mediationInterstitialListener;

  // Banner objects.
  private AppLovinAdView adView;

  // Controlled fields.
  private String zoneId;

  // Flag to let multiple loading of ads
  private boolean enableMultipleAdLoading = false;

  // region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener interstitialListener,
      @NonNull final Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable final Bundle networkExtras) {

    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid SDK Key.",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      interstitialListener.onAdFailedToLoad(ApplovinAdapter.this, error);
      return;
    }

    if (AppLovinUtils.isMultiAdsEnabled(serverParameters)) {
      enableMultipleAdLoading = true;
    }

    AppLovinInitializer.getInstance()
        .initialize(context, sdkKey, new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess(@NonNull String sdkKey) {
            zoneId = AppLovinUtils.retrieveZoneId(serverParameters);
            if (appLovinInterstitialAds.containsKey(zoneId)
                && appLovinInterstitialAds.get(zoneId).get() != null) {
              AdError error = new AdError(ERROR_AD_ALREADY_REQUESTED,
                  " Cannot load multiple interstitial ads with the same Zone ID. "
                      + "Display one ad before attempting to load another. ", ERROR_DOMAIN);
              log(ERROR, error.getMessage());
              interstitialListener.onAdFailedToLoad(ApplovinAdapter.this, error);
              return;
            }
            appLovinInterstitialAds.put(zoneId, new WeakReference<>(ApplovinAdapter.this));

            // Store parent objects.
            sdk = AppLovinInitializer.getInstance().retrieveSdk(serverParameters, context);
            ApplovinAdapter.this.context = context;
            ApplovinAdapter.this.networkExtras = networkExtras;
            mediationInterstitialListener = interstitialListener;

            log(DEBUG, "Requesting interstitial for zone: " + zoneId);

            // Create Ad Load listener.
            final AppLovinAdLoadListener adLoadListener =
                new AppLovinAdLoadListener() {
                  @Override
                  public void adReceived(final AppLovinAd ad) {
                    log(DEBUG,
                        "Interstitial did load ad: for zone: "
                            + zoneId);
                    appLovinInterstitialAd = ad;
                    if (enableMultipleAdLoading) {
                      unregister();
                    }

                    AppLovinSdkUtils.runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            mediationInterstitialListener.onAdLoaded(ApplovinAdapter.this);
                          }
                        });
                  }

                  @Override
                  public void failedToReceiveAd(final int code) {
                    AdError error = AppLovinUtils.getAdError(code);
                    log(WARN, error.getMessage());
                    ApplovinAdapter.this.unregister();
                    AppLovinSdkUtils.runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
                            mediationInterstitialListener
                                .onAdFailedToLoad(ApplovinAdapter.this, error);
                          }
                        });
                  }
                };

            if (!TextUtils.isEmpty(zoneId)) {
              sdk.getAdService().loadNextAdForZoneId(zoneId, adLoadListener);
            } else {
              sdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, adLoadListener);
            }
          }
        });
  }

  @Override
  public void showInterstitial() {
    // Update mute state.
    sdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(networkExtras));

    final AppLovinInterstitialAdDialog interstitialAdDialog =
        AppLovinInterstitialAd.create(sdk, context);

    final AppLovinInterstitialAdListener listener =
        new AppLovinInterstitialAdListener(ApplovinAdapter.this, mediationInterstitialListener);
    interstitialAdDialog.setAdDisplayListener(listener);
    interstitialAdDialog.setAdClickListener(listener);
    interstitialAdDialog.setAdVideoPlaybackListener(listener);

    if (appLovinInterstitialAd == null) {
      log(DEBUG, "Attempting to show interstitial before one was loaded.");

      // Check if we have a default zone interstitial available.
      if (TextUtils.isEmpty(zoneId)) {
        log(DEBUG, "Showing interstitial preloaded by SDK.");
        interstitialAdDialog.show();
      }
      // TODO: Show ad for zone identifier if exists
      else {
        mediationInterstitialListener.onAdOpened(this);
        mediationInterstitialListener.onAdClosed(this);
      }
      return;
    }

    log(DEBUG, "Showing interstitial for zone: " + zoneId);
    interstitialAdDialog.showAndRender(appLovinInterstitialAd);
  }
  // endregion

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener mediationBannerListener,
      @NonNull final Bundle serverParameters, @NonNull final AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle networkExtras) {

    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid SDK Key.",
          ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, error);
      return;
    }

    // Convert requested size to AppLovin Ad Size.
    final AppLovinAdSize appLovinAdSize =
        AppLovinUtils.appLovinAdSizeFromAdMobAdSize(context, adSize);
    if (appLovinAdSize == null) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to request banner with unsupported size.", ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, error);
      return;
    }

    AppLovinInitializer.getInstance()
        .initialize(context, sdkKey, new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess(@NonNull String sdkKey) {
            // Store parent objects
            sdk = AppLovinInitializer.getInstance().retrieveSdk(serverParameters, context);
            zoneId = AppLovinUtils.retrieveZoneId(serverParameters);

            log(DEBUG, "Requesting banner of size " + appLovinAdSize + " for zone: " + zoneId);
            adView = new AppLovinAdView(sdk, appLovinAdSize, context);

            final AppLovinBannerAdListener listener = new AppLovinBannerAdListener(zoneId, adView,
                ApplovinAdapter.this, mediationBannerListener);
            adView.setAdDisplayListener(listener);
            adView.setAdClickListener(listener);
            adView.setAdViewEventListener(listener);

            if (!TextUtils.isEmpty(zoneId)) {
              sdk.getAdService().loadNextAdForZoneId(zoneId, listener);
            } else {
              sdk.getAdService().loadNextAd(appLovinAdSize, listener);
            }
          }
        });
  }

  @NonNull
  @Override
  public View getBannerView() {
    return adView;
  }
  // endregion

  // region MediationAdapter.
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

  // OnContextChangedListener Method.
  @Override
  public void onContextChanged(@NonNull Context context) {
    log(DEBUG, "Context changed: " + context);
    this.context = context;
  }

  // Logging
  public static void log(int priority, final String message) {
    if (LOGGING_ENABLED) {
      Log.println(priority, "AppLovinAdapter", message);
    }
  }

  // Utilities
  void unregister() {
    if (!TextUtils.isEmpty(zoneId)
        && appLovinInterstitialAds.containsKey(zoneId)
        && ApplovinAdapter.this.equals(appLovinInterstitialAds.get(zoneId).get())) {
      appLovinInterstitialAds.remove(zoneId);
    }
  }
}
