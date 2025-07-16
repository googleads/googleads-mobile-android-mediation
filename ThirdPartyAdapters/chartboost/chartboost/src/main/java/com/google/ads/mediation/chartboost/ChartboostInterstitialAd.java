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

import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.ads.Interstitial;
import com.chartboost.sdk.callbacks.InterstitialCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.chartboost.sdk.impl.z7;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class ChartboostInterstitialAd implements MediationInterstitialAd, InterstitialCallback {

  private Interstitial chartboostInterstitialAd;

  private final MediationInterstitialAdConfiguration interstitialAdConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;
  private MediationInterstitialAdCallback interstitialAdCallback;

  public ChartboostInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd,
          MediationInterstitialAdCallback> mediationAdLoadCallback) {
    this.interstitialAdConfiguration = mediationInterstitialAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void loadAd() {
    final Context context = interstitialAdConfiguration.getContext();
    Bundle serverParameters = interstitialAdConfiguration.getServerParameters();

    ChartboostParams chartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters);
    if (!ChartboostAdapterUtils.isValidChartboostParams(chartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load interstitial ad from Chartboost. Missing or invalid server"
                  + " parameters.");
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    final String location = chartboostParams.getLocation();
    ChartboostAdapterUtils.updateCoppaStatus(context, interstitialAdConfiguration.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .initialize(context, chartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            createAndLoadInterstitialAd(location);
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
          }
        });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (chartboostInterstitialAd == null || !chartboostInterstitialAd.isCached()) {
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_AD_NOT_READY,
              "Chartboost interstitial ad is not yet ready to be shown.");
      Log.w(TAG, error.toString());
      return;
    }
    chartboostInterstitialAd.show();
  }

  private void createAndLoadInterstitialAd(@Nullable String location) {
    if (TextUtils.isEmpty(location)) {
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Missing or invalid location.");
      Log.w(TAG, error.toString());
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    chartboostInterstitialAd = new Interstitial(location, ChartboostInterstitialAd.this,
        ChartboostAdapterUtils.getChartboostMediation());
    chartboostInterstitialAd.cache();
  }

  @Override
  public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
    Log.d(TAG, "Chartboost interstitial ad has been dismissed.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
    Log.d(TAG, "Chartboost interstitial ad impression recorded.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
    if (showError == null) {
      Log.d(TAG, "Chartboost interstitial has been shown.");
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdOpened();
      }
    } else {
      AdError error = ChartboostConstants.createSDKError(showError);
      Log.w(TAG, error.toString());
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdFailedToShow(error);
      }
    }
  }

  @Override
  public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
    Log.d(TAG, "Chartboost interstitial ad is requested to be shown.");
  }

  @Override
  public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
    if (cacheError == null) {
      Log.d(TAG, "Chartboost interstitial ad has been loaded.");
      if (mediationAdLoadCallback != null) {
        interstitialAdCallback =
            mediationAdLoadCallback.onSuccess(ChartboostInterstitialAd.this);
      }
    } else {
      AdError error = ChartboostConstants.createSDKError(cacheError);
      Log.w(TAG, error.toString());
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
      }
    }
  }

  @Override
  public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
    if (clickError == null) {
      Log.d(TAG, "Chartboost interstitial ad has been clicked.");
      if (interstitialAdCallback != null) {
        interstitialAdCallback.reportAdClicked();
      }
    } else {
      AdError error = ChartboostConstants.createSDKError(clickError);
      Log.w(TAG, error.toString());
    }
  }

  @Override
  public void onAdExpired(@NonNull z7 ad) {
    Log.d(TAG, "Chartboost interstitial ad Expired.");
  }
}
