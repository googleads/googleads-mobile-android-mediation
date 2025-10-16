// Copyright 2025 Google LLC
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

package com.google.ads.mediation.vungle.renderers;

import static com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleFactory;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BaseAd;
import com.vungle.ads.VungleAdSize;
import com.vungle.ads.VungleBannerView;
import com.vungle.ads.VungleError;
import com.vungle.mediation.VungleInterstitialAdapter;

/**
 * Abstract class with banner adapter logic that is common for both waterfall and RTB integrations.
 */
public abstract class VungleBannerAd implements MediationBannerAd, BannerAdListener {

  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      mediationAdLoadCallback;
  private MediationBannerAdCallback mediationBannerAdCallback;

  private VungleBannerView bannerAdView;

  private final VungleFactory vungleFactory;

  public VungleBannerAd(
      @NonNull
          MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
              mediationAdLoadCallback,
      VungleFactory vungleFactory) {
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.vungleFactory = vungleFactory;
  }

  public void validateParamsAndLoadAd(
      MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load bidding banner ad from Liftoff Monetize. "
              + "Missing or invalid App ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placementForPlay = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placementForPlay)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load bidding banner ad from Liftoff Monetize. "
              + "Missing or Invalid Placement ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationBannerAdConfiguration.getContext();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    VungleAdSize bannerAdSize =
        VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(adSize, placementForPlay);

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                createBannerViewAndLoadAd(
                    context, placementForPlay, bannerAdSize, mediationBannerAdConfiguration);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  private void createBannerViewAndLoadAd(
      Context context,
      String placementId,
      VungleAdSize bannerAdSize,
      MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    bannerAdView = vungleFactory.createBannerAd(context, placementId, bannerAdSize);
    bannerAdView.setAdListener(this);
    loadAd(bannerAdView, mediationBannerAdConfiguration);
  }

  protected abstract void loadAd(
      VungleBannerView bannerAdView, MediationBannerAdConfiguration mediationBannerAdConfiguration);

  @NonNull
  @Override
  public View getView() {
    return bannerAdView;
  }

  @Override
  public void onAdClicked(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdClicked();
      mediationBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdEnd(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdImpression(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdLoaded(@NonNull BaseAd baseAd) {
    mediationBannerAdCallback = mediationAdLoadCallback.onSuccess(this);
  }

  @Override
  public void onAdStart(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    Log.w(TAG, error.toString());
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    Log.w(TAG, error.toString());
    mediationAdLoadCallback.onFailure(error);
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    if (mediationBannerAdCallback != null) {
      mediationBannerAdCallback.onAdLeftApplication();
    }
  }

}
