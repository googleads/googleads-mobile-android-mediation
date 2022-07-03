package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberAdapterUtils.getAdError;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.*;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * Class for rendering a Fyber Marketplace rewarded video.
 */
public class FyberRewardedVideoRenderer implements MediationRewardedAd {

  /**
   * AdMob's Rewarded ad configuration object.
   */
  private MediationRewardedAdConfiguration mAdConfiguration;

  /**
   * AdMob's callback object.
   */
  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mAdLoadCallback;

  /**
   * AbMob's rewarded ad callback. as returned from {@link MediationAdLoadCallback#onSuccess}.
   */
  private MediationRewardedAdCallback mRewardedAdCallback;

  /**
   * The Spot object for the banner.
   */
  private InneractiveAdSpot mRewardedSpot;
  private InneractiveFullscreenUnitController mUnitController;

  /**
   * Constructor.
   *
   * @param adConfiguration AdMob interstitial ad configuration.
   * @param adLoadCallback  AdMob load callback.
   */
  FyberRewardedVideoRenderer(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
    mAdConfiguration = adConfiguration;
    mAdLoadCallback = adLoadCallback;
  }

  void render() {
    // Check that we got a valid Spot ID from the server.
    String spotId = mAdConfiguration.getServerParameters()
        .getString(FyberMediationAdapter.KEY_SPOT_ID);
    if (TextUtils.isEmpty(spotId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Spot ID is null or empty.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mAdLoadCallback.onFailure(error);
      return;
    }

    mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
    mRewardedSpot.setMediationName(InneractiveMediationName.ADMOB);

    mUnitController = new InneractiveFullscreenUnitController();
    mRewardedSpot.addUnitController(mUnitController);

    InneractiveAdSpot.RequestListener requestListener = createRequestListener();
    mRewardedSpot.setRequestListener(requestListener);

    FyberAdapterUtils.updateFyberUserParams(mAdConfiguration.getMediationExtras());
    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    mRewardedSpot.requestAd(request);
  }

  private InneractiveAdSpot.RequestListener createRequestListener() {
    return new InneractiveAdSpot.RequestListener() {
      @Override
      public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
        // Report load success to AdMob, and cache the returned callback for a later use
        mRewardedAdCallback = mAdLoadCallback.onSuccess(FyberRewardedVideoRenderer.this);
        registerFyberAdListener(mUnitController);
      }

      @Override
      public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
          InneractiveErrorCode errorCode) {
        AdError error = getAdError(errorCode);
        Log.w(TAG, error.getMessage());
        mAdLoadCallback.onFailure(error);
      }
    };
  }

  /**
   * Creates a listener for Fyber's fullscreen placement events.
   *
   * @param controller the full screen controller.
   */
  private void registerFyberAdListener(
      final @NonNull InneractiveFullscreenUnitController controller) {

    InneractiveFullscreenAdEventsListenerAdapter adListener =
        new InneractiveFullscreenAdEventsListenerAdapter() {
          @Override
          public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
            mRewardedAdCallback.onAdOpened();

            // Code review note: Report video start should be called before reporting ad impression
            if (isVideoAdAvailable(controller)) {
              mRewardedAdCallback.onVideoStart();
            }

            mRewardedAdCallback.reportAdImpression();
          }

          @Override
          public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
            mRewardedAdCallback.reportAdClicked();
          }

          @Override
          public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
            mRewardedAdCallback.onAdClosed();
          }
        };

    // If the ad is a video ad, wait for the video completion event.
    final InneractiveFullscreenVideoContentController videoContentController =
        new InneractiveFullscreenVideoContentController();
    controller.setEventsListener(adListener);
    // Official rewarded interface for both Video and display ads (Since Marketplace 7.6.0)
    controller.setRewardedListener(new InneractiveFullScreenAdRewardedListener() {
      @Override
      public void onAdRewarded(InneractiveAdSpot inneractiveAdSpot) {
        mRewardedAdCallback.onUserEarnedReward(RewardItem.DEFAULT_REWARD);
        mRewardedAdCallback.onVideoComplete();
      }
    });

    controller.addContentController(videoContentController);
  }

  @Override
  public void showAd(@NonNull Context context) {
    // We need an activity context to show rewarded ads.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE,
          "Cannot show a rewarded ad without an activity context.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (mRewardedAdCallback != null) {
        mRewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    if (mRewardedSpot != null && mUnitController != null && mRewardedSpot.isReady()) {
      mUnitController.show((Activity) context);
    } else if (mRewardedAdCallback != null) {
      AdError error = new AdError(ERROR_AD_NOT_READY, "Fyber's rewarded spot is not ready.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mRewardedAdCallback.onAdFailedToShow(error);
    }
  }

  /**
   * Checks if the given unit controller, contains a video ad.
   *
   * @param controller a populated content controller.
   * @return true if a video ad is available, false otherwise.
   */
  private boolean isVideoAdAvailable(InneractiveFullscreenUnitController controller) {
    return controller != null
        && controller.getSelectedContentController() != null
        && controller.getSelectedContentController() instanceof
        InneractiveFullscreenVideoContentController;
  }
}
