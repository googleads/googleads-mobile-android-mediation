package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_AD_NOT_READY_TO_SHOW;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.YAHOO_MOBILE_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeYahooSDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.interstitialplacement.InterstitialAd;
import com.yahoo.ads.interstitialplacement.InterstitialAd.InterstitialAdListener;
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig;
import com.yahoo.ads.utils.ThreadUtils;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class YahooRewardedRenderer implements InterstitialAdListener,
    MediationRewardedAd {

  /**
   * Yahoo Mobile SDK specific event ID for video completion.
   */
  private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";

  /**
   * The mediation ad load callback.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Yahoo rewarded ad.
   */
  private InterstitialAd rewardedAd;

  /**
   * Flag to check 'onEvent()' completion.
   */
  private final AtomicBoolean completionEventCalled = new AtomicBoolean();

  /**
   * The mediation rewarded ad callback used to report ad event callbacks.
   */
  private MediationRewardedAdCallback mediationRewardedAdCallback;

  /**
   * The mediation rewarded ad configuration.
   */
  private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;

  public YahooRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
  }

  public void render() {
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String siteId = YahooAdapterUtils.getSiteId(serverParameters,
        mediationRewardedAdConfiguration);
    Context context = mediationRewardedAdConfiguration.getContext();

    if (TextUtils.isEmpty(siteId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Site ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      mediationAdLoadCallback.onFailure(parameterError);
    }

    AdError initializationError = initializeYahooSDK(context, siteId);
    if (initializationError != null) {
      Log.w(TAG, initializationError.toString());
      mediationAdLoadCallback.onFailure(initializationError);
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Placement ID.", ERROR_DOMAIN);
      Log.e(TAG, parameterError.toString());
      mediationAdLoadCallback.onFailure(parameterError);
      return;
    }

    YahooAdapterUtils.setCoppaValue(mediationRewardedAdConfiguration);

    InterstitialPlacementConfig placementConfig = new InterstitialPlacementConfig(placementId,
        YahooAdapterUtils.getRequestMetaData(mediationRewardedAdConfiguration));
    rewardedAd = new InterstitialAd(context, placementId, YahooRewardedRenderer.this);
    rewardedAd.load(placementConfig);
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (rewardedAd == null) {
      AdError showError = new AdError(ERROR_AD_NOT_READY_TO_SHOW, "No ads ready to be shown.",
          ERROR_DOMAIN);
      Log.w(TAG, showError.toString());
      if (mediationRewardedAdCallback != null) {
        mediationRewardedAdCallback.onAdFailedToShow(showError);
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

  // region Yahoo InterstitialAdListener implementation.

  @Override
  public void onLoaded(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Mobile SDK loaded a rewarded ad successfully.");
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

  @Override
  public void onLoadFailed(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    AdError loadError = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, loadError.toString());
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(loadError);
        }
      }
    });
  }

  @Override
  public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    AdError error = new AdError(errorInfo.getErrorCode(), errorInfo.getDescription(),
        YAHOO_MOBILE_SDK_ERROR_DOMAIN);
    Log.w(TAG, error.toString());

    // This error callback is used if the interstitial ad is loaded successfully, but an
    // error occurs while trying to display
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationRewardedAdCallback != null) {
          mediationRewardedAdCallback.onAdFailedToShow(error);
        }
      }
    });
  }

  @Override
  public void onShown(final InterstitialAd interstitialAd) {
    Log.i(TAG, "Yahoo Mobile SDK showed a rewarded ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK closed a rewarded ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK recoded a click on a rewarded ad.");
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
    Log.i(TAG, "Yahoo Mobile SDK has caused the user to leave the application "
        + "from a rewarded ad.");
  }

  @Override
  public void onEvent(final InterstitialAd interstitialAd, final String source,
      final String eventId, final Map<String, Object> arguments) {

    if (VIDEO_COMPLETE_EVENT_ID.equals(eventId) && !completionEventCalled.getAndSet(true)) {
      Log.i(TAG, "Yahoo Mobile SDK has completed playing a rewarded ad.");
      ThreadUtils.postOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onVideoComplete();
            mediationRewardedAdCallback.onUserEarnedReward(new RewardItem() {
              @NonNull
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

  // endregion
}
