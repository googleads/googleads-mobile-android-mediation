// Copyright 2024 Google LLC
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

package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceConstants.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.TAG;
import static com.google.ads.mediation.ironsource.IronSourceConstants.WATERMARK;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_CALL_SHOW_BEFORE_LOADED_SUCCESS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_REQUIRES_ACTIVITY_CONTEXT;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.unity3d.ironsourceads.interstitial.InterstitialAd;
import com.unity3d.ironsourceads.interstitial.InterstitialAdListener;
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoader;
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoaderListener;
import com.unity3d.ironsourceads.interstitial.InterstitialAdRequest;

public class IronSourceRtbInterstitialAd
    implements MediationInterstitialAd, InterstitialAdLoaderListener, InterstitialAdListener {

  @VisibleForTesting private MediationInterstitialAdCallback interstitialAdCallback;

  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;

  private final String instanceID;

  private final String bidToken;

  private InterstitialAd ad = null;

  private final String watermark;

  public IronSourceRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration interstitialAdConfig,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationInterstitialAdLoadCallback) {
    Bundle serverParameters = interstitialAdConfig.getServerParameters();
    instanceID = serverParameters.getString(KEY_INSTANCE_ID, "");
    bidToken = interstitialAdConfig.getBidResponse();
    watermark = interstitialAdConfig.getWatermark();
    mediationAdLoadCallback = mediationInterstitialAdLoadCallback;
  }

  /** Attempts to load an @{link IronSource} interstitial ad using a Bid token. */
  public void loadRtbAd() {
    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.");
      mediationAdLoadCallback.onFailure(loadError);
      return;
    }

    Bundle watermarkBundle = new Bundle();
    watermarkBundle.putString(WATERMARK, watermark);

    InterstitialAdRequest adRequest =
        new InterstitialAdRequest.Builder(instanceID, bidToken)
            .withExtraParams(watermarkBundle)
            .build();
    InterstitialAdLoader.loadAd(adRequest, this);
  }

  @Override
  public void showAd(@NonNull Context context) {

    if (ad == null) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_CALL_SHOW_BEFORE_LOADED_SUCCESS, "ad is null");
      reportAdFailedToShow(contextError);
      return;
    }

    try {

      Activity activity = (Activity) context;
      ad.setListener(this);
      ad.show(activity);

    } catch (ClassCastException e) {
      AdError contextError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_REQUIRES_ACTIVITY_CONTEXT,
              "IronSource requires an Activity context to load ads.");
      reportAdFailedToShow(contextError);
    }
  }

  private void reportAdFailedToShow(@NonNull AdError showError) {
    Log.e(TAG, showError.toString());
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdFailedToShow(showError);
    }
  }

  @Override
  public void onInterstitialAdClicked(@NonNull InterstitialAd interstitialAd) {
    if (interstitialAdCallback == null) {
      return;
    }
    interstitialAdCallback.reportAdClicked();
  }

  @Override
  public void onInterstitialAdDismissed(@NonNull InterstitialAd interstitialAd) {
    if (interstitialAdCallback == null) {
      return;
    }
    interstitialAdCallback.onAdClosed();
  }

  @Override
  public void onInterstitialAdFailedToShow(
      @NonNull InterstitialAd interstitialAd, @NonNull IronSourceError ironSourceError) {
    AdError adError =
        IronSourceAdapterUtils.buildAdErrorIronSourceDomain(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    reportAdFailedToShow(adError);
  }

  @Override
  public void onInterstitialAdShown(@NonNull final InterstitialAd interstitialAd) {
    if (interstitialAdCallback == null) {
      return;
    }

    interstitialAdCallback.onAdOpened();
    interstitialAdCallback.reportAdImpression();
  }

  @Override
  public void onInterstitialAdLoadFailed(@NonNull IronSourceError ironSourceError) {
    Log.e(TAG, ironSourceError.toString());
    AdError adError =
        IronSourceAdapterUtils.buildAdErrorIronSourceDomain(
            ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
    mediationAdLoadCallback.onFailure(adError);
  }

  @Override
  public void onInterstitialAdLoaded(@NonNull InterstitialAd interstitialAd) {
    ad = interstitialAd;
    interstitialAdCallback = mediationAdLoadCallback.onSuccess(this);
  }
}
