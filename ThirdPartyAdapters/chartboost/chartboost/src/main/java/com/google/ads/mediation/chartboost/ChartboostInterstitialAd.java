package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostAdapter.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.chartboost.ChartboostAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.chartboost.ChartboostAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.chartboost.ChartboostAdapter.TAG;

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

public class ChartboostInterstitialAd implements MediationInterstitialAd {

  private Interstitial mChartboostInterstitialAd;

  private final MediationInterstitialAdConfiguration mInterstitialAdConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mMediationAdLoadCallback;
  private MediationInterstitialAdCallback mInterstitialAdCallback;

  public ChartboostInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd,
          MediationInterstitialAdCallback> mediationAdLoadCallback) {
    mInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
    mMediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void load() {
    final Context context = mInterstitialAdConfiguration.getContext();
    Bundle serverParameters = mInterstitialAdConfiguration.getServerParameters();

    ChartboostParams mChartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters, null);
    if (!ChartboostAdapterUtils.isValidChartboostParams(mChartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Invalid server parameters.",
          ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    final String location = mChartboostParams.getLocation();
    ChartboostInitializer.getInstance()
        .updateCoppaStatus(context, mInterstitialAdConfiguration.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .init(context, mChartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            createAndLoadInterstitialAd(location, mMediationAdLoadCallback);
          }

          @Override
          public void onInitializationFailed(@NonNull AdError error) {
            Log.w(TAG, error.getMessage());
            if (mMediationAdLoadCallback != null) {
              mMediationAdLoadCallback.onFailure(error);
            }
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

  private void createAndLoadInterstitialAd(@Nullable String location,
      @Nullable final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mMediationAdLoadCallback) {
    if (TextUtils.isEmpty(location)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or Invalid location.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mMediationAdLoadCallback != null) {
        mMediationAdLoadCallback.onFailure(error);
      }
      return;
    }

    mChartboostInterstitialAd = new Interstitial(location,
        new InterstitialCallback() {
          @Override
          public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
            Log.d(TAG, "Chartboost interstitial ad has been dismissed.");
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.onAdClosed();
            }
          }

          @Override
          public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
            Log.d(TAG, "Chartboost interstitial ad impression recorded.");
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
            if (showError == null) {
              Log.d(TAG, "Chartboost interstitial has been shown.");
              if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdOpened();
              }
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(showError);
              Log.w(TAG, error.getMessage());
              if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.onAdFailedToShow(error);
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
              if (mMediationAdLoadCallback != null) {
                mInterstitialAdCallback =
                    mMediationAdLoadCallback.onSuccess(ChartboostInterstitialAd.this);
              }
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(cacheError);
              Log.w(TAG, error.getMessage());
              if (mMediationAdLoadCallback != null) {
                mMediationAdLoadCallback.onFailure(error);
              }
            }
          }

          @Override
          public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
            if (clickError == null) {
              Log.d(TAG, "Chartboost interstitial ad has been clicked.");
              if (mInterstitialAdCallback != null) {
                mInterstitialAdCallback.reportAdClicked();
              }
            } else {
              AdError error = ChartboostAdapterUtils.createSDKError(clickError);
              Log.w(TAG, error.getMessage());
            }
          }
        }, ChartboostAdapterUtils.getChartboostMediation());
    mChartboostInterstitialAd.cache();
  }
}
