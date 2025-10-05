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

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.getAdSizeFromGoogleAdSize;
import static com.google.ads.mediation.ironsource.IronSourceConstants.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceConstants.WATERMARK;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.unity3d.ironsourceads.banner.BannerAdLoader;
import com.unity3d.ironsourceads.banner.BannerAdLoaderListener;
import com.unity3d.ironsourceads.banner.BannerAdRequest;
import com.unity3d.ironsourceads.banner.BannerAdView;
import com.unity3d.ironsourceads.banner.BannerAdViewListener;

/**
 * Used to load ironSource RTB Banner ads and mediate callbacks between Google Mobile Ads SDK and
 * ironSource SDK.
 */
public class IronSourceRtbBannerAd
    implements MediationBannerAd, BannerAdLoaderListener, BannerAdViewListener {

  @VisibleForTesting private MediationBannerAdCallback adLifecycleCallback;

  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;

  private FrameLayout ironSourceAdView;

  public IronSourceRtbBannerAd(
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback) {
    adLoadCallback = mediationAdLoadCallback;
  }

  public void loadRtbAd(@NonNull MediationBannerAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String instanceID = serverParameters.getString(KEY_INSTANCE_ID, "");
    if (TextUtils.isEmpty(instanceID)) {
      AdError loadError =
          IronSourceAdapterUtils.buildAdErrorAdapterDomain(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid instance ID.");
      adLoadCallback.onFailure(loadError);
      return;
    }

    String watermark = adConfiguration.getWatermark();
    Bundle watermarkBundle = new Bundle();
    watermarkBundle.putString(WATERMARK, watermark);

    Context context = adConfiguration.getContext();
    AdSize adSize = adConfiguration.getAdSize();
    String bidToken = adConfiguration.getBidResponse();
    BannerAdRequest adRequest =
        new BannerAdRequest.Builder(
                context, instanceID, bidToken, getAdSizeFromGoogleAdSize(context, adSize))
            .withExtraParams(watermarkBundle)
            .build();
    ironSourceAdView = new FrameLayout(context);
    BannerAdLoader.loadAd(adRequest, this);
  }

  @NonNull
  @Override
  public View getView() {
    return ironSourceAdView;
  }

  @Override
  public void onBannerAdLoaded(@NonNull BannerAdView bannerAdView) {
    if (this.ironSourceAdView == null || this.adLoadCallback == null) {
      return;
    }
    bannerAdView.setListener(this);
    this.ironSourceAdView.addView(bannerAdView);
    adLifecycleCallback = adLoadCallback.onSuccess(this);
  }

  @Override
  public void onBannerAdLoadFailed(@NonNull IronSourceError ironSourceError) {
    if (adLoadCallback == null) {
      return;
    }
    final AdError loadError =
        new AdError(
            ironSourceError.getErrorCode(),
            ironSourceError.getErrorMessage(),
            IRONSOURCE_SDK_ERROR_DOMAIN);
    adLoadCallback.onFailure(loadError);
  }

  @Override
  public void onBannerAdClicked(@NonNull BannerAdView bannerAdView) {
    if (adLifecycleCallback == null) {
      return;
    }
    adLifecycleCallback.onAdOpened();
    adLifecycleCallback.reportAdClicked();
  }

  @Override
  public void onBannerAdShown(@NonNull BannerAdView bannerAdView) {
    if (adLifecycleCallback != null) {
      adLifecycleCallback.reportAdImpression();
    }
  }
}
