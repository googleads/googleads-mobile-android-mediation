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

package com.google.ads.mediation.imobile;

import static com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import jp.co.imobile.sdkads.android.FailNotificationReason;
import jp.co.imobile.sdkads.android.ImobileSdkAd;
import jp.co.imobile.sdkads.android.ImobileSdkAdListener;

/** i-mobile mediation adapter for AdMob banner and interstitial ads. */
public final class IMobileAdapter extends IMobileMediationAdapter
    implements MediationInterstitialAdapter {

  // region - Fields for log.
  /** Tag for log. */
  static final String TAG = IMobileAdapter.class.getSimpleName();

  // endregion

  // region - Fields for interstitial ads.
  /**
   * Listener for interstitial ads.
   */
  private MediationInterstitialListener mediationInterstitialListener;

  /**
   * Activity to display interstitial ads.
   */
  private Activity interstitialActivity;

  /**
   * i-mobile spot ID.
   */
  private String interstitialSpotId;
  // endregion

  // region - Methods for interstitial ads.
  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener listener, @NonNull Bundle serverParameters,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {

    // Validate Context.
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Context is not an Activity.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(this, error);
      return;
    }
    interstitialActivity = (Activity) context;

    // Initialize fields.
    mediationInterstitialListener = listener;

    // Get parameters for i-mobile SDK.
    String publisherId = serverParameters.getString(Constants.KEY_PUBLISHER_ID);
    String mediaId = serverParameters.getString(Constants.KEY_MEDIA_ID);
    interstitialSpotId = serverParameters.getString(Constants.KEY_SPOT_ID);

    // Call i-mobile SDK.
    ImobileSdkAd.registerSpotFullScreen(
        interstitialActivity, publisherId, mediaId, interstitialSpotId);
    ImobileSdkAd.setImobileSdkAdListener(
        interstitialSpotId,
        new ImobileSdkAdListener() {
          @Override
          public void onAdReadyCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdShowCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdOpened(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdCliclkCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdClicked(IMobileAdapter.this);
              mediationInterstitialListener.onAdLeftApplication(IMobileAdapter.this);
            }
          }

          @Override
          public void onAdCloseCompleted() {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdClosed(IMobileAdapter.this);
            }
          }

          @Override
          public void onFailed(FailNotificationReason reason) {
            AdError error = AdapterHelper.getAdError(reason);
            Log.w(TAG, error.getMessage());
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdFailedToLoad(IMobileAdapter.this, error);
            }
          }
        });

    // Start getting ads.
    if (ImobileSdkAd.isShowAd(interstitialSpotId)) {
      mediationInterstitialListener.onAdLoaded(IMobileAdapter.this);
    } else {
      ImobileSdkAd.start(interstitialSpotId);
    }
  }

  @Override
  public void showInterstitial() {
    // Show ad.
    if (interstitialActivity != null
        && interstitialActivity.hasWindowFocus()
        && interstitialSpotId != null) {
      ImobileSdkAd.showAdforce(interstitialActivity, interstitialSpotId);
    }
  }
  // endregion

  // region - Methods of life cycle.
  @Override
  public void onDestroy() {
    // Release objects.
    mediationInterstitialListener = null;
    interstitialActivity = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }
  // endregion

}