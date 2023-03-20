// Copyright 2022 Google LLC
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

package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.google.ads.mediation.pangle.PangleAdapterUtils;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleInitializer;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

public class PangleRtbInterstitialAd implements MediationInterstitialAd {

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;
  private MediationInterstitialAdCallback interstitialAdCallback;
  private PAGInterstitialAd pagInterstitialAd;

  public PangleRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback) {
    adConfiguration = mediationInterstitialAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleAdapterUtils.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementId = serverParameters.getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load interstitial ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_BID_RESPONSE,
              "Failed to load interstitial ad from Pangle. Missing or invalid bid response.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    Context context = adConfiguration.getContext();
    String appId = serverParameters.getString(PangleConstants.APP_ID);
    PangleInitializer.getInstance()
        .initialize(
            context,
            appId,
            new Listener() {
              @Override
              public void onInitializeSuccess() {
                PAGInterstitialRequest request = new PAGInterstitialRequest();
                request.setAdString(bidResponse);
                PAGInterstitialAd.loadAd(
                    placementId,
                    request,
                    new PAGInterstitialAdLoadListener() {
                      @Override
                      public void onError(int errorCode, String errorMessage) {
                        AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
                        Log.w(TAG, error.toString());
                        adLoadCallback.onFailure(error);
                      }

                      @Override
                      public void onAdLoaded(PAGInterstitialAd interstitialAd) {
                        interstitialAdCallback =
                            adLoadCallback.onSuccess(PangleRtbInterstitialAd.this);
                        pagInterstitialAd = interstitialAd;
                      }
                    });
              }

              @Override
              public void onInitializeError(@NonNull AdError error) {
                Log.w(TAG, error.toString());
                adLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    pagInterstitialAd.setAdInteractionListener(
        new PAGInterstitialAdInteractionListener() {
          @Override
          public void onAdShowed() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.onAdOpened();
              interstitialAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdClicked() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdDismissed() {
            if (interstitialAdCallback != null) {
              interstitialAdCallback.onAdClosed();
            }
          }
        });

    if (context instanceof Activity) {
      pagInterstitialAd.show((Activity) context);
      return;
    }
    // If the context is not an Activity, the application context will be used to render the ad.
    pagInterstitialAd.show(null);
  }
}
