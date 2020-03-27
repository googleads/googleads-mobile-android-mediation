package com.google.ads.mediation.fyber;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.VideoContentListenerAdapter;
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
   * @param adLoadCallback AdMob load callback.
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
      String logMessage = "Cannot render rewarded ad. " +
          "Please define a valid spot id on the AdMob UI.";
      Log.e(FyberMediationAdapter.TAG, logMessage);
      mAdLoadCallback.onFailure(logMessage);
      return;
    }

    mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
    mRewardedSpot.setMediationName(InneractiveMediationName.ADMOB);

    mUnitController = new InneractiveFullscreenUnitController();
    mRewardedSpot.addUnitController(mUnitController);

    InneractiveAdSpot.RequestListener requestListener = createRequestListener();
    mRewardedSpot.setRequestListener(requestListener);

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
        String logMessage = "Fyber rewarded video request failed. Error: " + errorCode.toString();
        if (errorCode != InneractiveErrorCode.NO_FILL
            && errorCode != InneractiveErrorCode.CONNECTION_ERROR) {
          Log.w(FyberMediationAdapter.TAG, logMessage);
        }

        mAdLoadCallback.onFailure(logMessage);
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

            // Check for the returned ad type to send back the reward callback properly.
            // For video ads, the reward is earned only if the video is completed.
            // For Display ads, the reward is earned when the ad is dismissed.
            if (!isVideoAdAvailable(controller)) {
              mRewardedAdCallback.onUserEarnedReward(RewardItem.DEFAULT_REWARD);
            }
          }
        };

    // If the ad is a video ad, wait for the video completion event.
    final InneractiveFullscreenVideoContentController videoContentController =
        new InneractiveFullscreenVideoContentController();

    videoContentController.setEventsListener(new VideoContentListenerAdapter() {
      /**
       * Called by inneractive when a rewarded video ad was played to the end.
       * <br>Note: This event does not indicate that the rewarded video was closed.
       */
      @Override
      public void onCompleted() {
        mRewardedAdCallback.onVideoComplete();

        // The video is completed. an end card is shown.
        // The ad is not dismissed yet, but a reward is in order.
        mRewardedAdCallback.onUserEarnedReward(RewardItem.DEFAULT_REWARD);
      }
    });

    controller.addContentController(videoContentController);
    controller.setEventsListener(adListener);
  }

  @Override
  public void showAd(Context context) {
    if (mRewardedSpot != null && mUnitController != null && mRewardedSpot.isReady()) {
      mUnitController.show(context);
    } else if (mRewardedAdCallback != null) {
      mRewardedAdCallback.onAdFailedToShow("showAd called, " +
          "but Fyber's rewarded spot is not ready.");
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
