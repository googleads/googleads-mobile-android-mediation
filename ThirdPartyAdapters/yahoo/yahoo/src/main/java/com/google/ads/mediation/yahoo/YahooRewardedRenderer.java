package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.initializeSDK;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
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
    if (!initializeSDK(context, siteId)) {
      final String message = "Unable to initialize the Yahoo Mobile SDK.";
      Log.e(TAG, message);
      mediationAdLoadCallback.onFailure(message);
      return;
    }

    String placementId = YahooAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      mediationAdLoadCallback.onFailure("Yahoo Mobile SDK placement ID must be set in "
          + "mediationRewardedAdConfiguration server params.");
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
    final String message = "Yahoo Mobile SDK failed to request a rewarded ad with error code: "
        + errorInfo.getErrorCode() + ", message: " + errorInfo.getDescription();
    Log.w(TAG, message);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationAdLoadCallback != null) {
          mediationAdLoadCallback.onFailure(message);
        }
      }
    });
  }

  @Override
  public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
    Log.w(TAG, "Yahoo Mobile SDK returned an error for rewarded ad: " + errorInfo);

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
