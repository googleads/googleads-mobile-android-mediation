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

import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_MISSING_SDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * The {@link AppLovinBannerAd} is used to load Unity Banner ads and mediate the callbacks between
 * Google Mobile Ads SDK and AppLovin SDK.
 */
public class AppLovinBannerAd
    implements MediationBannerAd,
        AppLovinAdLoadListener,
        AppLovinAdDisplayListener,
        AppLovinAdClickListener,
        AppLovinAdViewEventListener {

  private AppLovinAdViewWrapper appLovinAdViewWrapper;

  // Parent objects.
  private AppLovinSdk sdk;
  private Context context;

  // Controlled fields.
  private String zoneId;

  private final AppLovinInitializer appLovinInitializer;
  private final AppLovinAdFactory appLovinAdFactory;
  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationAdLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;

  private static final String TAG = AppLovinBannerAd.class.getSimpleName();

  private AppLovinBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.appLovinInitializer = appLovinInitializer;
    this.appLovinAdFactory = appLovinAdFactory;
  }

  public static AppLovinBannerAd newInstance(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory) {
    return new AppLovinBannerAd(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback,
        appLovinInitializer,
        appLovinAdFactory);
  }

  /**
   * Attempts to initialize AppLovin Sdk through the {@link AppLovinInitializer} class and
   * communicates the response through the {@link MediationAdLoadCallback} declared when
   * instanciating the Banner class
   */
  public void loadAd() {
    context = mediationBannerAdConfiguration.getContext();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();
    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, ERROR_MSG_MISSING_SDK, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    // Convert requested size to AppLovin Ad Size.
    final AppLovinAdSize appLovinAdSize =
        AppLovinUtils.appLovinAdSizeFromAdMobAdSize(context, adSize);
    if (appLovinAdSize == null) {
      AdError error =
          new AdError(ERROR_BANNER_SIZE_MISMATCH, ERROR_MSG_BANNER_SIZE_MISMATCH, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    appLovinInitializer.initialize(
        context,
        sdkKey,
        new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess(@NonNull String sdkKey) {
            // Store parent objects
            sdk = appLovinInitializer.retrieveSdk(serverParameters, context);
            zoneId = AppLovinUtils.retrieveZoneId(serverParameters);

            Log.d(TAG, "Requesting banner of size " + appLovinAdSize + " for zone: " + zoneId);
            appLovinAdViewWrapper = appLovinAdFactory.createAdView(sdk, appLovinAdSize, context);

            appLovinAdViewWrapper.setAdDisplayListener(AppLovinBannerAd.this);
            appLovinAdViewWrapper.setAdClickListener(AppLovinBannerAd.this);
            appLovinAdViewWrapper.setAdViewEventListener(AppLovinBannerAd.this);

            if (!TextUtils.isEmpty(zoneId)) {
              sdk.getAdService().loadNextAdForZoneId(zoneId, AppLovinBannerAd.this);
            } else {
              sdk.getAdService().loadNextAd(appLovinAdSize, AppLovinBannerAd.this);
            }
          }
        });
  }

  @NonNull
  @Override
  public View getView() {
    return appLovinAdViewWrapper.getAppLovinAdView();
  }

  @Override
  public void adReceived(final AppLovinAd ad) {
    Log.d(TAG, "Banner did load ad for zone: " + zoneId);
    appLovinAdViewWrapper.renderAd(ad);
    bannerAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void failedToReceiveAd(final int code) {
    AdError error = AppLovinUtils.getAdError(code);
    Log.w(TAG, "Failed to load banner ad with error: " + code);
    mediationAdLoadCallback.onFailure(error);
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    Log.d(TAG, "Banner displayed.");
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    Log.d(TAG, "Banner dismissed.");
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    Log.d(TAG, "Banner clicked.");
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  // Ad View Event Listener.
  @Override
  public void adOpenedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner opened fullscreen.");
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void adClosedFullscreen(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner closed fullscreen.");
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }

  @Override
  public void adLeftApplication(AppLovinAd ad, AppLovinAdView adView) {
    Log.d(TAG, "Banner left application.");
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void adFailedToDisplay(
      AppLovinAd ad, AppLovinAdView adView, AppLovinAdViewDisplayErrorCode code) {
    Log.w(TAG, "Banner failed to display: " + code);
  }
}
