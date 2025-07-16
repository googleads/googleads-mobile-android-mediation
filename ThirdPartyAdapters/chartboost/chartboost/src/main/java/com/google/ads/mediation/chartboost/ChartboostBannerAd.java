// Copyright 2022 Google LLC
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

package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.chartboost.sdk.impl.z7;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class ChartboostBannerAd implements MediationBannerAd, BannerCallback {

  /**
   * A container view that holds Chartboost's {@link Banner} view.
   */
  private FrameLayout bannerContainer;

  private final MediationBannerAdConfiguration bannerAdConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationAdLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;

  public ChartboostBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd,
          MediationBannerAdCallback> mediationAdLoadCallback) {
    this.bannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void loadAd() {
    final Context context = bannerAdConfiguration.getContext();
    Bundle serverParameters = bannerAdConfiguration.getServerParameters();

    ChartboostParams chartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters);
    if (!ChartboostAdapterUtils.isValidChartboostParams(chartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load banner ad from Chartboost. Missing or invalid server parameters.");
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }
    AdSize adSize = bannerAdConfiguration.getAdSize();
    Banner.BannerSize supportedAdSize = ChartboostAdapterUtils.findClosestBannerSize(context,
        adSize);
    if (supportedAdSize == null) {
      String errorMessage = String.format(
          "The requested banner size: %s is not supported by Chartboost SDK.", adSize);
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              errorMessage);
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    final String location = chartboostParams.getLocation();
    ChartboostAdapterUtils.updateCoppaStatus(context, bannerAdConfiguration.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .initialize(context, chartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            createAndLoadBannerAd(context, location, supportedAdSize);
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
          }
        });
  }

  private void createAndLoadBannerAd(@NonNull Context context,
      @Nullable String location,
      @Nullable Banner.BannerSize supportedAdSize) {
    if (TextUtils.isEmpty(location)) {
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid location.");
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    bannerContainer = new FrameLayout(context);
    AdSize closestSize = new AdSize(supportedAdSize.getWidth(), supportedAdSize.getHeight());
    FrameLayout.LayoutParams paramsLayout =
        new FrameLayout.LayoutParams(
            closestSize.getWidthInPixels(context), closestSize.getHeightInPixels(context));
    Banner chartboostBannerAd = new Banner(context, location, supportedAdSize,
        ChartboostBannerAd.this, ChartboostAdapterUtils.getChartboostMediation());
    bannerContainer.addView(chartboostBannerAd, paramsLayout);
    // Chartboost banner requires cache call to be loaded. It has to be done before show call.
    chartboostBannerAd.cache();
  }

  @Override
  public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
    Log.d(TAG, "Chartboost banner ad impression recorded.");
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
    if (showError != null) {
      AdError error = ChartboostConstants.createSDKError(showError);
      Log.w(TAG, error.toString());
      return;
    }

    Log.d(TAG, "Chartboost banner has been shown.");
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
    Log.d(TAG, "Chartboost banner ad is requested to be shown.");
  }

  @Override
  public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
    if (cacheError != null) {
      AdError error = ChartboostConstants.createSDKError(cacheError);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Log.d(TAG, "Chartboost banner ad has been loaded.");
    bannerAdCallback =
        mediationAdLoadCallback.onSuccess(ChartboostBannerAd.this);
    cacheEvent.getAd().show();
  }

  @Override
  public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
    if (clickError != null) {
      AdError error = ChartboostConstants.createSDKError(clickError);
      Log.w(TAG, error.toString());
      return;
    }

    Log.d(TAG, "Chartboost banner ad has been clicked.");
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdExpired(@NonNull z7 ad) {
    Log.d(TAG, "Chartboost banner ad Expired.");
  }

  @NonNull
  @Override
  public View getView() {
    return bannerContainer;
  }
}
