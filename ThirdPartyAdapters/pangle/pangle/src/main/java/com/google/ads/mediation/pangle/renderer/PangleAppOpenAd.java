// Copyright 2023 Google LLC
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

package com.google.ads.mediation.pangle.renderer;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleFactory;
import com.google.ads.mediation.pangle.PangleInitializer;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.ads.mediation.pangle.PanglePrivacyConfig;
import com.google.ads.mediation.pangle.PangleRequestHelper;
import com.google.ads.mediation.pangle.PangleSdkWrapper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;

public class PangleAppOpenAd implements MediationAppOpenAd {

  @VisibleForTesting
  static final String ERROR_MSG_INVALID_PLACEMENT_ID =
      "Failed to load app open ad from Pangle. Missing or invalid Placement ID.";

  private final MediationAppOpenAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
      adLoadCallback;
  private final PangleInitializer pangleInitializer;
  private final PangleSdkWrapper pangleSdkWrapper;
  private final PangleFactory pangleFactory;
  private final PanglePrivacyConfig panglePrivacyConfig;

  private MediationAppOpenAdCallback appOpenAdCallback;
  private PAGAppOpenAd pagAppOpenAd;

  public PangleAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration mediationAppOpenAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleSdkWrapper pangleSdkWrapper,
      @NonNull PangleFactory pangleFactory,
      @NonNull PanglePrivacyConfig panglePrivacyConfig) {
    adConfiguration = mediationAppOpenAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
    this.pangleInitializer = pangleInitializer;
    this.pangleSdkWrapper = pangleSdkWrapper;
    this.pangleFactory = pangleFactory;
    this.panglePrivacyConfig = panglePrivacyConfig;
  }

  public void render() {
    panglePrivacyConfig.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    Bundle serverParameters = adConfiguration.getServerParameters();
    final String placementId = serverParameters.getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS, ERROR_MSG_INVALID_PLACEMENT_ID);
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    final String bidResponse = adConfiguration.getBidResponse();
    Context context = adConfiguration.getContext();
    String appId = serverParameters.getString(PangleConstants.APP_ID);
    pangleInitializer.initialize(
        context,
        appId,
        new Listener() {
          @Override
          public void onInitializeSuccess() {
            PAGAppOpenRequest request = pangleFactory.createPagAppOpenRequest();
            request.setAdString(bidResponse);
            PangleRequestHelper.setWatermarkString(request, bidResponse, adConfiguration);
            pangleSdkWrapper.loadAppOpenAd(
                placementId,
                request,
                new PAGAppOpenAdLoadListener() {
                  @Override
                  public void onError(int errorCode, String errorMessage) {
                    AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
                    Log.w(TAG, error.toString());
                    adLoadCallback.onFailure(error);
                  }

                  @Override
                  public void onAdLoaded(PAGAppOpenAd appOpenAd) {
                    appOpenAdCallback = adLoadCallback.onSuccess(PangleAppOpenAd.this);
                    pagAppOpenAd = appOpenAd;
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
    pagAppOpenAd.setAdInteractionListener(
        new PAGAppOpenAdInteractionListener() {
          @Override
          public void onAdShowed() {
            if (appOpenAdCallback != null) {
              appOpenAdCallback.onAdOpened();
              appOpenAdCallback.reportAdImpression();
            }
          }

          @Override
          public void onAdClicked() {
            if (appOpenAdCallback != null) {
              appOpenAdCallback.reportAdClicked();
            }
          }

          @Override
          public void onAdDismissed() {
            if (appOpenAdCallback != null) {
              appOpenAdCallback.onAdClosed();
            }
          }
        });
    if (context instanceof Activity) {
      pagAppOpenAd.show((Activity) context);
      return;
    }
    // If the context is not an Activity, the application context will be used to render the ad.
    pagAppOpenAd.show(null);
  }
}
