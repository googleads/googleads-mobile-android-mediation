package com.google.ads.mediation.verizon;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;
import static com.google.ads.mediation.verizon.VerizonMediationAdapter.initializeSDK;

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
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;
import com.verizon.ads.utils.ThreadUtils;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class VerizonMediaRewardedRenderer implements InterstitialAd.InterstitialAdListener,
    InterstitialAdFactory.InterstitialAdFactoryListener, MediationRewardedAd {

  private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";

  /**
   * The mediation ad load callback.
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mediationAdLoadCallback;

  /**
   * Verizon Media rewarded ad.
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

  public VerizonMediaRewardedRenderer(@NonNull MediationAdLoadCallback<MediationRewardedAd,
      MediationRewardedAdCallback> mediationAdLoadCallback, MediationRewardedAdConfiguration
      mediationRewardedAdConfiguration) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
  }

  public void render() {
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
    String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters,
        mediationRewardedAdConfiguration);
    Context context = mediationRewardedAdConfiguration.getContext();
    if (!initializeSDK(context, siteId)) {
      final String message = "Unable to initialize Verizon Ads SDK.";
      Log.e(TAG, message);
      mediationAdLoadCallback.onFailure(message);
      return;
    }

    String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
    if (TextUtils.isEmpty(placementId)) {
      mediationAdLoadCallback.onFailure(
          "Verizon Ads SDK placement ID must be set in mediationRewardedAdConfiguration" +
              " server params.");
      return;
    }

    VerizonMediaAdapterUtils.setCoppaValue(mediationRewardedAdConfiguration);
    VASAds.setLocationEnabled((mediationRewardedAdConfiguration.getLocation() != null));
    InterstitialAdFactory interstitialAdFactory =
        new InterstitialAdFactory(mediationRewardedAdConfiguration.getContext(),
            placementId, this);
    interstitialAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
        .getRequestMetaData(mediationRewardedAdConfiguration));
    interstitialAdFactory.load(this);
  }

  @Override
  public void onLoaded(final InterstitialAdFactory interstitialAdFactory,
      final InterstitialAd interstitialAd) {

    Log.i(TAG, "Verizon Ads SDK incentivized video interstitial loaded.");
    this.rewardedAd = interstitialAd;
    // Reset the completion event with each new interstitial ad load.
    completionEventCalled.set(false);
    ThreadUtils.postOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mediationAdLoadCallback != null) {
          mediationRewardedAdCallback =
              mediationAdLoadCallback.onSuccess(VerizonMediaRewardedRenderer.this);
        }
      }
    });
  }

  @Override
  public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory,
      final int numRequested, final int numReceived) {
    // no op.  caching not supported in adapter
  }

  @Override
  public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory,
      final int cacheSize) {
    // no op.  caching not supported in adapter
  }

  @Override
  public void onError(final InterstitialAdFactory interstitialAdFactory,
      final ErrorInfo errorInfo) {
    final String message = "Verizon Ads SDK incentivized video interstitial request failed (" +
        errorInfo.getErrorCode() + "): " + errorInfo.getDescription();
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

    Log.e(TAG, "Verizon Ads SDK incentivized video interstitial error: " + errorInfo);

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
    Log.i(TAG, "Verizon Ads SDK incentivized video interstitial shown.");
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
    Log.i(TAG, "Verizon Ads SDK ad closed.");
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
    Log.i(TAG, "Verizon Ads SDK incentivized video interstitial clicked.");
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
    Log.i(TAG, "Verizon Ads SDK incentivized video interstitial left application.");
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
