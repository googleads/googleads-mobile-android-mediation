package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
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
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class ChartboostInterstitialAd implements MediationInterstitialAd, InterstitialCallback {

  private Interstitial mChartboostInterstitialAd;

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

    ChartboostParams mChartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Invalid server parameters.",
          ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    final String location = mChartboostParams.getLocation();
    ChartboostInitializer.getInstance()
        .updateCoppaStatus(context, interstitialAdConfiguration.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .init(context, mChartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            createAndLoadInterstitialAd(location);
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
          }
        });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (mChartboostInterstitialAd == null || !mChartboostInterstitialAd.isCached()) {
      AdError error = new AdError(ERROR_AD_NOT_READY,
          "Chartboost interstitial ad is not yet ready to be shown.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      return;
    }
    mChartboostInterstitialAd.show();
  }

  private void createAndLoadInterstitialAd(@Nullable String location) {
    if (TextUtils.isEmpty(location)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid location.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mediationAdLoadCallback != null) {
        mediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    mChartboostInterstitialAd = new Interstitial(location, ChartboostInterstitialAd.this,
        ChartboostAdapterUtils.getChartboostMediation());
    mChartboostInterstitialAd.cache();
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
      AdError error = ChartboostAdapterUtils.createSDKError(showError);
      Log.w(TAG, error.getMessage());
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdFailedToShow(error);
      }
    }
  }

  @Override
  public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
    Log.d(TAG, "Chartboost interstitial ad will be shown.");
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
      AdError error = ChartboostAdapterUtils.createSDKError(cacheError);
      Log.w(TAG, error.getMessage());
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
      AdError error = ChartboostAdapterUtils.createSDKError(clickError);
      Log.w(TAG, error.getMessage());
    }
  }
}
