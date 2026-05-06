// Copyright 2017 Google LLC
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

package com.google.ads.mediation.mytarget;

import static com.google.ads.mediation.mytarget.MyTargetTools.handleMediationExtras;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.my.target.ads.InterstitialAd;
import com.my.target.common.CustomParams;
import com.my.target.common.models.IAdLoadingError;

/** Mediation adapter for myTarget. */
public class MyTargetAdapter extends MyTargetMediationAdapter
    implements MediationInterstitialAdapter {

  @NonNull
  private static final String TAG = "MyTargetAdapter";

  @Nullable
  private InterstitialAd mInterstitial;

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener mediationInterstitialListener,
      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {
    int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
    Log.d(TAG, "Requesting myTarget interstitial mediation with Slot ID: " + slotId);

    if (slotId < 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationInterstitialListener.onAdFailedToLoad(MyTargetAdapter.this, error);
      return;
    }

    MyTargetInterstitialListener interstitialListener = new MyTargetInterstitialListener(
        mediationInterstitialListener);

    if (mInterstitial != null) {
      mInterstitial.destroy();
    }

    mInterstitial = new InterstitialAd(slotId, context);
    CustomParams params = mInterstitial.getCustomParams();
    handleMediationExtras(TAG, mediationExtras, params);
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
    mInterstitial.setListener(interstitialListener);
    mInterstitial.load();
  }

  @Override
  public void showInterstitial() {
    if (mInterstitial != null) {
      mInterstitial.show();
    }
  }

  @Override
  public void onDestroy() {
    if (mInterstitial != null) {
      mInterstitial.destroy();
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  /**
   * A {@link MyTargetInterstitialListener} used to forward myTarget interstitial events to Google.
   */
  private class MyTargetInterstitialListener implements InterstitialAd.InterstitialAdListener {

    @NonNull
    private final MediationInterstitialListener listener;

    MyTargetInterstitialListener(final @NonNull MediationInterstitialListener listener) {
      this.listener = listener;
    }

    @Override
    public void onLoad(@NonNull final InterstitialAd ad) {
      Log.d(TAG, "Interstitial mediation Ad loaded.");
      listener.onAdLoaded(MyTargetAdapter.this);
    }

    @Override
    public void onNoAd(@NonNull final IAdLoadingError reason, @NonNull final InterstitialAd ad) {
      AdError error = 
          new AdError(ERROR_MY_TARGET_SDK, reason.getMessage(), MY_TARGET_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      listener.onAdFailedToLoad(MyTargetAdapter.this, error);
    }

    @Override
    public void onClick(@NonNull final InterstitialAd ad) {
      Log.d(TAG, "Interstitial mediation Ad clicked.");
      listener.onAdClicked(MyTargetAdapter.this);
      // click redirects user to Google Play, or web browser, so we can notify
      // about left application.
      listener.onAdLeftApplication(MyTargetAdapter.this);
    }

    @Override
    public void onDismiss(@NonNull final InterstitialAd ad) {
      Log.d(TAG, "Interstitial mediation Ad dismissed.");
      listener.onAdClosed(MyTargetAdapter.this);
    }

    @Override
    public void onVideoCompleted(@NonNull final InterstitialAd ad) {
    }

    @Override
    public void onDisplay(@NonNull final InterstitialAd ad) {
      Log.d(TAG, "Interstitial mediation Ad displayed.");
      listener.onAdOpened(MyTargetAdapter.this);
    }

    @Override
    public void onFailedToShow(@NonNull InterstitialAd interstitialAd) {
      AdError error =
          new AdError(ERROR_AD_FAILED_TO_SHOW, ERROR_MSG_AD_FAILED_TO_SHOW, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
    }
  }
}
