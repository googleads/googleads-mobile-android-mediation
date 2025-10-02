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

package com.google.ads.mediation.facebook.rtb;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_ADVIEW_CONSTRUCTOR_EXCEPTION;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getAdError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.facebook.ads.Ad;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdView;
import com.facebook.ads.ExtraHints;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.ads.mediation.facebook.MetaFactory;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

public class FacebookRtbBannerAd implements MediationBannerAd, AdListener {

  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
  private AdView adView;
  private FrameLayout wrappedAdView;
  private MediationBannerAdCallback bannerAdCallback;

  private final MetaFactory metaFactory;

  public FacebookRtbBannerAd(
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback,
      MetaFactory metaFactory) {
    this.callback = callback;
    this.metaFactory = metaFactory;
  }

  public void render(@NonNull MediationBannerAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementID = FacebookMediationAdapter.getPlacementID(serverParameters);
    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    setMixedAudience(adConfiguration);
    try {
      adView = metaFactory.createMetaAdView(adConfiguration.getContext(), placementID,
          adConfiguration.getBidResponse());
    } catch (Exception exception) {
      AdError error = new AdError(ERROR_ADVIEW_CONSTRUCTOR_EXCEPTION,
          "Failed to create banner ad: " + exception.getMessage(), ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    if (!TextUtils.isEmpty(adConfiguration.getWatermark())) {
      adView.setExtraHints(
          new ExtraHints.Builder().mediationData(adConfiguration.getWatermark()).build());
    }

    Context context = adConfiguration.getContext();
    FrameLayout.LayoutParams adViewLayoutParams = new FrameLayout.LayoutParams(
        adConfiguration.getAdSize().getWidthInPixels(context), LayoutParams.WRAP_CONTENT);
    wrappedAdView = new FrameLayout(context);
    adView.setLayoutParams(adViewLayoutParams);
    wrappedAdView.addView(adView);
    adView.loadAd(
        adView.buildLoadAdConfig()
            .withAdListener(this)
            .withBid(adConfiguration.getBidResponse())
            .build()
    );
  }

  @NonNull
  @Override
  public View getView() {
    return wrappedAdView;
  }

  @Override
  public void onError(Ad ad, com.facebook.ads.AdError adError) {
    AdError error = getAdError(adError);
    Log.w(TAG, error.getMessage());
    callback.onFailure(error);
  }

  @Override
  public void onAdLoaded(Ad ad) {
    bannerAdCallback = callback.onSuccess(this);
  }

  @Override
  public void onAdClicked(Ad ad) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
      bannerAdCallback.onAdOpened();
      bannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onLoggingImpression(Ad ad) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }
}
