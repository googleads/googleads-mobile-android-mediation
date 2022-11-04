package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.ads.Rewarded;
import com.chartboost.sdk.callbacks.RewardedCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

public class ChartboostRewardedAd implements MediationRewardedAd, RewardedCallback {

  private Rewarded chartboostRewardedAd;

  private final MediationRewardedAdConfiguration rewardedAdConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;
  private MediationRewardedAdCallback rewardedAdCallback;

  public ChartboostRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd,
          MediationRewardedAdCallback> mediationAdLoadCallback) {
    this.rewardedAdConfiguration = mediationRewardedAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void loadAd() {
    final Context context = rewardedAdConfiguration.getContext();
    Bundle serverParameters = rewardedAdConfiguration.getServerParameters();

    ChartboostParams chartboostParams =
        ChartboostAdapterUtils.createChartboostParams(serverParameters);
    if (!ChartboostAdapterUtils.isValidChartboostParams(chartboostParams)) {
      // Invalid server parameters, send ad failed to load event.
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load rewarded ad from Chartboost. Missing or invalid server parameters.");
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    final String location = chartboostParams.getLocation();
    ChartboostAdapterUtils.updateCoppaStatus(context, rewardedAdConfiguration.taggedForChildDirectedTreatment());
    ChartboostInitializer.getInstance()
        .initialize(context, chartboostParams, new ChartboostInitializer.Listener() {
          @Override
          public void onInitializationSucceeded() {
            chartboostRewardedAd = new Rewarded(location, ChartboostRewardedAd.this,
                ChartboostAdapterUtils.getChartboostMediation());
            chartboostRewardedAd.cache();
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
    if (chartboostRewardedAd == null || !chartboostRewardedAd.isCached()) {
      AdError error =
          ChartboostConstants.createAdapterError(
              ERROR_AD_NOT_READY,
              "Chartboost rewarded ad is not yet ready to be shown.");
      Log.w(TAG, error.toString());
      return;
    }
    chartboostRewardedAd.show();
  }

  @Override
  public void onRewardEarned(@NonNull RewardEvent rewardEvent) {
    Log.d(TAG, "User earned a rewarded from Chartboost rewarded ad.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onVideoComplete();
      rewardedAdCallback.onUserEarnedReward(new RewardItem() {
        @Override
        public int getAmount() {
          return rewardEvent.getReward();
        }

        @NonNull
        @Override
        public String getType() {
          // Charboost doesn't provide reward type.
          return "";
        }
      });
    }
  }

  @Override
  public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
    Log.d(TAG, "Chartboost rewarded ad has been dismissed.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.onAdClosed();
    }
  }

  @Override
  public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
    Log.d(TAG, "Chartboost rewarded ad impression recorded.");
    if (rewardedAdCallback != null) {
      rewardedAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
    if (showError == null) {
      Log.d(TAG, "Chartboost rewarded ad has been shown.");
      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdOpened();
        rewardedAdCallback.onVideoStart();
      }
    } else {
      AdError error = ChartboostConstants.createSDKError(showError);
      Log.w(TAG, error.toString());
      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
    }
  }

  @Override
  public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
    Log.d(TAG, "Chartboost rewarded ad is requested to be shown.");
  }

  @Override
  public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
    if (cacheError == null) {
      Log.d(TAG, "Chartboost rewarded ad has been loaded.");
      if (mediationAdLoadCallback != null) {
        rewardedAdCallback =
            mediationAdLoadCallback.onSuccess(ChartboostRewardedAd.this);
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
      Log.d(TAG, "Chartboost rewarded ad has been clicked.");
      if (rewardedAdCallback != null) {
        rewardedAdCallback.reportAdClicked();
      }
    } else {
      AdError error = ChartboostConstants.createSDKError(clickError);
      Log.w(TAG, error.toString());
    }
  }
}
