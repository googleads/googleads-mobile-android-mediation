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

package com.google.ads.mediation.maio;

import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.maio.MaioMediationAdapter.TAG;
import static com.google.ads.mediation.maio.MaioMediationAdapter.getAdError;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import java.util.ArrayList;
import java.util.List;
import jp.maio.sdk.android.mediation.admob.adapter.MaioAdsManager;
import jp.maio.sdk.android.v2.banner.MaioBannerListener;
import jp.maio.sdk.android.v2.banner.MaioBannerSize;
import jp.maio.sdk.android.v2.banner.MaioBannerView;

public class MaioBannerAd implements MediationBannerAd {
  private MaioBannerView bannerView;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;

  public MaioBannerAd(
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback) {
    this.adLoadCallback = adLoadCallback;
  }

  public void loadAd(MediationBannerAdConfiguration mediationBannerAdConfiguration) {
    Context context = mediationBannerAdConfiguration.getContext();

    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();
    String mediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mediaID)) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    String zoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(zoneID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    MaioBannerSize bannerSize =
        toMaioBannerSize(context, mediationBannerAdConfiguration.getAdSize());
    if (bannerSize == null) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "The requested ad size is not supported by maio SDK.",
              ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      adLoadCallback.onFailure(error);
      return;
    }

    bannerView = new MaioBannerView(context, zoneID, bannerSize);
    bannerView.setListener(new MaioBannerListener() {
      @Override
      public void loaded(@NonNull MaioBannerView maioBannerView) {
        if (adLoadCallback != null) {
          bannerAdCallback = adLoadCallback.onSuccess(MaioBannerAd.this);
        }
      }

      @Override
      public void failedToLoad(@NonNull MaioBannerView maioBannerView, int errorCode) {
        if (adLoadCallback != null) {
          Log.w(TAG, getAdError(errorCode).getMessage());
          adLoadCallback.onFailure(getAdError(errorCode));
        }
      }

      @Override
      public void impression(@NonNull MaioBannerView maioBannerView) {
        if (bannerAdCallback != null) {
          bannerAdCallback.reportAdImpression();
        }
      }

      @Override
      public void clicked(@NonNull MaioBannerView maioBannerView) {
        if (bannerAdCallback != null) {
          bannerAdCallback.reportAdClicked();
        }
      }

      @Override
      public void leftApplication(@NonNull MaioBannerView maioBannerView) {
        if (bannerAdCallback != null) {
          bannerAdCallback.onAdLeftApplication();
        }
      }

      @Override
      public void failedToShow(@NonNull MaioBannerView maioBannerView, int errorCode) {
        if (adLoadCallback != null) {
          Log.w(TAG, getAdError(errorCode).getMessage());
          adLoadCallback.onFailure(getAdError(errorCode));
        }
      }
    });
    bannerView.load(mediationBannerAdConfiguration.isTestRequest());
  }

  @NonNull
  @Override
  public View getView() {
    return bannerView;
  }

  private MaioBannerSize toMaioBannerSize(Context context, AdSize adSize) {
    List<AdSize> supportedSizes = new ArrayList<>();
    supportedSizes.add(new AdSize(320, 50));
    supportedSizes.add(new AdSize(320, 100));
    supportedSizes.add(new AdSize(300, 250));
    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, supportedSizes);

    if (closestSize != null) {
      return new MaioBannerSize(closestSize.getWidth(), closestSize.getHeight());
    } else {
      return null;
    }
  }
}
