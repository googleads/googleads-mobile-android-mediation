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

package com.google.ads.mediation.pangle.renderer;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize;
import com.google.ads.mediation.pangle.PangleAdapterUtils;
import com.google.ads.mediation.pangle.PangleBannerAdLoader;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleInitializer;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import java.util.ArrayList;

public class PangleBannerAd implements MediationBannerAd, PAGBannerAdInteractionListener {

  @VisibleForTesting
  public static final String ERROR_MESSAGE_BANNER_SIZE_MISMATCH =
      "Failed to request banner ad from Pangle. Invalid banner size.";

  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;
  private final PangleInitializer pangleInitializer;
  private final PangleBannerAdLoader pangleBannerAdLoader;
  private MediationBannerAdCallback bannerAdCallback;
  private FrameLayout wrappedAdView;

  public PangleBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleBannerAdLoader pangleBannerAdLoader) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
    this.pangleInitializer = pangleInitializer;
    this.pangleBannerAdLoader = pangleBannerAdLoader;
  }

  public void render() {
    PangleAdapterUtils.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementId = serverParameters.getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load banner ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    Context context = adConfiguration.getContext();
    String appId = serverParameters.getString(PangleConstants.APP_ID);
    pangleInitializer
        .initialize(
            context,
            appId,
            new Listener() {
              @Override
              public void onInitializeSuccess() {
                ArrayList<AdSize> supportedSizes = new ArrayList<>(3);
                supportedSizes.add(new AdSize(320, 50));
                supportedSizes.add(new AdSize(300, 250));
                supportedSizes.add(new AdSize(728, 90));
                AdSize closestSize =
                    MediationUtils.findClosestSize(
                        context, adConfiguration.getAdSize(), supportedSizes);
                if (closestSize == null) {
                  AdError error =
                      PangleConstants.createAdapterError(
                          ERROR_BANNER_SIZE_MISMATCH, ERROR_MESSAGE_BANNER_SIZE_MISMATCH);
                  Log.w(TAG, error.toString());
                  adLoadCallback.onFailure(error);
                  return;
                }

                wrappedAdView = new FrameLayout(context);

                PAGBannerRequest request =
                    new PAGBannerRequest(
                        new PAGBannerSize(closestSize.getWidth(), closestSize.getHeight()));
                request.setAdString(bidResponse);
                pangleBannerAdLoader.loadAd(
                    placementId,
                    request,
                    new PAGBannerAdLoadListener() {
                      @Override
                      public void onError(int errorCode, String errorMessage) {
                        AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
                        Log.w(TAG, error.toString());
                        adLoadCallback.onFailure(error);
                      }

                      @Override
                      public void onAdLoaded(PAGBannerAd pagBannerAd) {
                        pagBannerAd.setAdInteractionListener(PangleBannerAd.this);
                        wrappedAdView.addView(pagBannerAd.getBannerView());
                        bannerAdCallback = adLoadCallback.onSuccess(PangleBannerAd.this);
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

  @NonNull
  @Override
  public View getView() {
    return wrappedAdView;
  }

  @Override
  public void onAdShowed() {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClicked() {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdDismissed() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
