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
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.MyTargetView;
import com.my.target.ads.MyTargetView.MyTargetViewListener;
import com.my.target.common.CustomParams;
import com.my.target.common.models.IAdLoadingError;

/**
 * Mediation adapter for myTarget.
 */
public class MyTargetAdapter extends MyTargetMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter {

  @NonNull
  private static final String TAG = "MyTargetAdapter";

  @Nullable
  private MyTargetView mMyTargetView;

  @Nullable
  private InterstitialAd mInterstitial;

  @Override
  public void requestBannerAd(@NonNull Context context,
      @NonNull MediationBannerListener mediationBannerListener, @NonNull Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
    Log.d(TAG, "Requesting myTarget banner mediation with Slot ID: " + slotId);

    if (slotId < 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(MyTargetAdapter.this, error);
      return;
    }

    MyTargetView.AdSize myTargetSize = MyTargetTools.getSupportedAdSize(adSize, context);
    if (myTargetSize == null) {
      String errorMessage = String.format("Unsupported ad size: %s.", adSize);
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH, errorMessage, ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(MyTargetAdapter.this, error);
      return;
    }

    String logMessage = String
        .format("Loading myTarget banner with size: %dx%d.", myTargetSize.getWidth(),
            myTargetSize.getHeight());
    Log.d(TAG, logMessage);
    MyTargetBannerListener bannerListener = new MyTargetBannerListener(mediationBannerListener);
    loadBanner(bannerListener, mediationAdRequest, slotId, myTargetSize, context, mediationExtras);
  }

  @NonNull
  @Override
  public View getBannerView() {
    return mMyTargetView;
  }

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
    if (mMyTargetView != null) {
      mMyTargetView.destroy();
    }
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
   * Starts loading banner.
   *
   * @param myTargetBannerListener listener for ad callbacks
   * @param mediationAdRequest     Google mediation request
   * @param slotId                 myTarget slot ID
   * @param adSize                 myTarget banner size.
   * @param context                app context
   */
  private void loadBanner(@NonNull MyTargetBannerListener myTargetBannerListener,
      @NonNull MediationAdRequest mediationAdRequest, int slotId,
      @NonNull MyTargetView.AdSize adSize, @NonNull Context context,
      @Nullable Bundle mediationExtras) {
    if (mMyTargetView != null) {
      mMyTargetView.destroy();
    }

    mMyTargetView = new MyTargetView(context);
    mMyTargetView.setSlotId(slotId);
    mMyTargetView.setAdSize(adSize);
    mMyTargetView.setRefreshAd(false);

    CustomParams params = mMyTargetView.getCustomParams();
    handleMediationExtras(TAG, mediationExtras, params);
    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);

    mMyTargetView.setListener(myTargetBannerListener);
    mMyTargetView.load();
  }

  /**
   * A {@link MyTargetBannerListener} used to forward myTarget banner events to Google.
   */
  private class MyTargetBannerListener implements MyTargetViewListener {

    @NonNull
    private final MediationBannerListener listener;

    MyTargetBannerListener(final @NonNull MediationBannerListener listener) {
      this.listener = listener;
    }

    @Override
    public void onLoad(@NonNull final MyTargetView view) {
      Log.d(TAG, "Banner mediation Ad loaded.");
      listener.onAdLoaded(MyTargetAdapter.this);
    }

    @Override
    public void onNoAd(@NonNull final IAdLoadingError reason, @NonNull final MyTargetView view) {
      AdError error = 
          new AdError(ERROR_MY_TARGET_SDK, reason.getMessage(), MY_TARGET_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      listener.onAdFailedToLoad(MyTargetAdapter.this, error);
    }

    @Override
    public void onShow(@NonNull MyTargetView view) {
      Log.d(TAG, "Banner mediation Ad show.");
    }

    @Override
    public void onClick(@NonNull final MyTargetView view) {
      Log.d(TAG, "Banner mediation Ad clicked.");
      listener.onAdClicked(MyTargetAdapter.this);
      listener.onAdOpened(MyTargetAdapter.this);
      // click redirects user to Google Play, or web browser, so we can notify
      // about left application.
      listener.onAdLeftApplication(MyTargetAdapter.this);
    }
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
  }
}
