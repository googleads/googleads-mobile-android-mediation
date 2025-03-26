// Copyright 2019 Google LLC
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

package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberAdapterUtils.getAdError;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.fyber.FyberMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot.RequestListener;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

/**
 * Class for rendering a DT Exchange rewarded video.
 */
public class FyberRewardedVideoRenderer implements MediationRewardedAd, RequestListener,
    InneractiveFullscreenAdEventsListener, InneractiveFullScreenAdRewardedListener {

  /**
   * AdMob's Rewarded ad configuration object.
   */
  private final MediationRewardedAdConfiguration adConfiguration;

  /**
   * AdMob's callback object.
   */
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      adLoadCallback;

  /**
   * AbMob's rewarded ad callback. as returned from {@link MediationAdLoadCallback#onSuccess}.
   */
  private MediationRewardedAdCallback rewardedAdCallback;

  /**
   * The Spot object for the rewarded ad.
   */
  private InneractiveAdSpot rewardedSpot;
  private InneractiveFullscreenUnitController unitController;

  /**
   * Constructor.
   *
   * @param adConfiguration AdMob interstitial ad configuration.
   * @param adLoadCallback  AdMob load callback.
   */
  FyberRewardedVideoRenderer(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = adLoadCallback;
  }

  void render() {
    // Check that we got a valid Spot ID from the server.
    String spotId = adConfiguration.getServerParameters()
        .getString(FyberMediationAdapter.KEY_SPOT_ID);
    if (TextUtils.isEmpty(spotId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Spot ID is null or empty.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    rewardedSpot = FyberFactory.createRewardedAdSpot();

    unitController = FyberFactory.createInneractiveFullscreenUnitController();
    rewardedSpot.addUnitController(unitController);

    rewardedSpot.setRequestListener(FyberRewardedVideoRenderer.this);

    FyberAdapterUtils.updateFyberExtraParams(adConfiguration.getMediationExtras());
    InneractiveAdRequest request = new InneractiveAdRequest(spotId);
    rewardedSpot.requestAd(request);
  }

  /**
   * Creates a listener for DT Exchange's fullscreen placement events.
   */
  private void registerFyberAdListeners() {
    unitController.setEventsListener(FyberRewardedVideoRenderer.this);
    // Official rewarded interface for both Video and display ads (Since Marketplace 7.6.0)
    unitController.setRewardedListener(FyberRewardedVideoRenderer.this);

    // If the ad is a video ad, wait for the video completion event.
    final InneractiveFullscreenVideoContentController videoContentController =
        new InneractiveFullscreenVideoContentController();
    unitController.addContentController(videoContentController);
  }

  @Override
  public void showAd(@NonNull Context context) {
    // We need an activity context to show rewarded ads.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_CONTEXT_NOT_ACTIVITY_INSTANCE,
          "Cannot show a rewarded ad without an activity context.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      if (rewardedAdCallback != null) {
        rewardedAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    if (rewardedSpot != null && unitController != null && rewardedSpot.isReady()) {
      unitController.show((Activity) context);
    } else if (rewardedAdCallback != null) {
      AdError error = new AdError(ERROR_AD_NOT_READY, "DT Exchange's rewarded spot is not ready.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      rewardedAdCallback.onAdFailedToShow(error);
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

  // region Fyber's RequestListener implementation
  @Override
  public void onInneractiveSuccessfulAdRequest(@NonNull InneractiveAdSpot adSpot) {
    // Report load success to AdMob, and cache the returned callback for a later use
    rewardedAdCallback = adLoadCallback.onSuccess(FyberRewardedVideoRenderer.this);
    registerFyberAdListeners();
  }

  @Override
  public void onInneractiveFailedAdRequest(@NonNull InneractiveAdSpot adSpot,
      @NonNull InneractiveErrorCode errorCode) {
    AdError error = getAdError(errorCode);
    Log.w(TAG, error.getMessage());
    adLoadCallback.onFailure(error);
  }
  // endregion

  // region Fyber's InneractiveFullscreenAdEventsListener implementation
  @Override
  public void onAdImpression(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    rewardedAdCallback.onAdOpened();

    // Code review note: Report video start should be called before reporting ad impression
    if (isVideoAdAvailable(unitController)) {
      rewardedAdCallback.onVideoStart();
    }

    rewardedAdCallback.reportAdImpression();
  }

  @Override
  public void onAdClicked(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    rewardedAdCallback.reportAdClicked();
  }

  @Override
  public void onAdDismissed(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    rewardedAdCallback.onAdClosed();
  }

  @Override
  public void onAdWillOpenExternalApp(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  @Override
  public void onAdEnteredErrorState(@NonNull InneractiveAdSpot inneractiveAdSpot,
      @NonNull AdDisplayError adDisplayError) {
    // No relevant events to be forwarded to the GMA SDK.
  }

  @Override
  public void onAdWillCloseInternalBrowser(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    // No relevant events to be forwarded to the GMA SDK.
  }
  // endregion

  // region Fyber's InneractiveFullScreenAdRewardedListener implementation
  @Override
  public void onAdRewarded(@NonNull InneractiveAdSpot inneractiveAdSpot) {
    rewardedAdCallback.onUserEarnedReward();
    rewardedAdCallback.onVideoComplete();
  }
  // endregion
}
