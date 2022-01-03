package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.interstitialplacement.InterstitialAd;
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig;
import com.yahoo.ads.placementcache.UnifiedAdManager;
import com.yahoo.ads.utils.ThreadUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class YahooRewardedRenderer implements InterstitialAd.InterstitialAdListener, MediationRewardedAd {

  private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";

  /**
   * The mediation ad load callback.
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Yahoo rewarded ad.
   */
  private InterstitialAd rewardedAd;

  /**
   * Flag to check 'onEvent()' completion.
   */
  private AtomicBoolean completionEventCalled = new AtomicBoolean();

  /**
   * The mediation rewarded ad callback used to report ad event callbacks.
   */
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  /**
   * The mediation rewarded ad configuration.
   */
  private MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  public YahooRewardedRenderer(@NonNull MediationAdLoadCallback<MediationRewardedAd,
      MediationRewardedAdCallback> mediationAdLoadCallback, MediationRewardedAdConfiguration
      mediationRewardedAdConfiguration) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
  }

  public void render() {
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String siteId = YahooAdapterUtils.getSiteId(serverParameters,
        mediationRewardedAdConfiguration);
    final Context context = mediationRewardedAdConfiguration.getContext();
    if (!initializeSDK(context, siteId)) {
      final String message = "Unable to initialize Yahoo Ads SDK.";
      Log.e(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, message, ERROR_DOMAIN);
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    final String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Yahoo Ads SDK placement ID must be set in mediationRewardedAdConfiguration" +
              " server params.", ERROR_DOMAIN);
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationRewardedAdConfiguration);
    YASAds.setLocationEnabled((mediationRewardedAdConfiguration.getLocation() != null));
    InterstitialPlacementConfig placementConfig = new InterstitialPlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetaData(mediationRewardedAdConfiguration));
    UnifiedAdManager.setPlacementConfig(placementId, placementConfig);
    UnifiedAdManager.fetchAds(context, placementId, new Function1<ErrorInfo, Unit>() {
        @Override
        public Unit invoke(final ErrorInfo errorInfo) {
        if (errorInfo != null) {
          onError(errorInfo);
        } else {
          onFetchComplete(context, placementId);
        }
        return null;
      }
    });
  }

  private void onFetchComplete(final Context context, final String placementId) {
    final InterstitialAd interstitialAd = new InterstitialAd(context, placementId, this);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        ErrorInfo errorInfo = interstitialAd.load();
        if (errorInfo != null) {
          onError(errorInfo);
        } else {
          onLoaded(interstitialAd);
        }
      }
    });
  }

  public void onLoaded(final InterstitialAd interstitialAd) {

    Log.i(TAG, "Yahoo Ads SDK incentivized video interstitial loaded.");
    this.rewardedAd = interstitialAd;
    // Reset the completion event with each new interstitial ad load.
    completionEventCalled.set(false);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationAdLoadCallback != null) {
          mediationRewardedAdCallback =
              mediationAdLoadCallback.onSuccess(YahooRewardedRenderer.this);
        }
      }
    });
  }


  public void onError(final ErrorInfo errorInfo) {
    final String message = "Yahoo Ads SDK incentivized video interstitial request failed (" +
        errorInfo.getErrorCode() + "): " + errorInfo.getDescription();
    Log.w(TAG, message);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationAdLoadCallback != null) {
          AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, message, ERROR_DOMAIN);
          mediationAdLoadCallback.onFailure(message);
        }
      }
    });
  }

  @Override
  public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

    Log.e(TAG, "Yahoo Ads SDK incentivized video interstitial error: " + errorInfo);

    // This error callback is used if the interstitial ad is loaded successfully, but an
    // error occurs while trying to display
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onAdFailedToShow(errorInfo.getDescription());
        }
      }
    });
  }

  @Override
  public void onShown(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK incentivized video interstitial shown.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onAdOpened();
          mediationRewardedAdCallback.onVideoStart();
        }
      }
    });
  }

  @Override
  public void onClosed(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK ad closed.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onAdClosed();
        }
      }
    });
  }

  @Override
  public void onClicked(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK incentivized video interstitial clicked.");
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.reportAdClicked();
        }
      }
    });
  }

  @Override
  public void onAdLeftApplication(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Ads SDK incentivized video interstitial left application.");
  }

  @Override
  public void onEvent(final InterstitialAd interstitialAd, final String source,
      final String eventId, final Map<String, Object> arguments) {

    if (VIDEO_COMPLETE_EVENT_ID.equals(eventId) && !completionEventCalled.getAndSet(true)) {
      ThreadUtils.postOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onVideoComplete();
            mediationRewardedAdCallback.onUserEarnedReward(new RewardItem() {
              @Override
              public String getType() {
                return "";
              }

              @Override
              public int getAmount() {
                return 1;
              }
            });
          }
        }
      });
    }
  }

  @Override
  public void showAd(Context context) {
    if (context == null) {
      if (mediationRewardedAdCallback != null) {
        mediationRewardedAdCallback.onAdFailedToShow("Failed to show: context is null.");
      }
      return;
    }

    if (rewardedAd == null) {
      if (mediationRewardedAdCallback != null) {
        mediationRewardedAdCallback.onAdFailedToShow("No ads to show.");
      }
      return;
    }

    rewardedAd.show(context);
  }

  void destroy() {
    if (rewardedAd != null) {
      rewardedAd.destroy();
    }
  }
}
