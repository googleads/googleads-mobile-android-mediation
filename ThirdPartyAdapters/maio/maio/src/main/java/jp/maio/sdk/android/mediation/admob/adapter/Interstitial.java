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

package jp.maio.sdk.android.mediation.admob.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.maio.MaioAdsManagerListener;
import com.google.ads.mediation.maio.MaioMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;

/**
 * maio mediation adapter for AdMob Interstitial videos.
 */
public class Interstitial extends MaioMediationAdapter
    implements MediationInterstitialAdapter, MaioAdsManagerListener {

  private MediationInterstitialListener mediationInterstitialListener;

  // region MediationInterstitialAdapter implementation
  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull MediationInterstitialListener listener, @NonNull Bundle serverParameters,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
    this.mediationInterstitialListener = listener;
    if (!(context instanceof Activity)) {
      AdError error = new AdError(ERROR_REQUIRES_ACTIVITY_CONTEXT,
          "Maio SDK requires an Activity context to load ads.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      return;
    }

    this.mediaID = serverParameters.getString(MaioAdsManager.KEY_MEDIA_ID);
    if (TextUtils.isEmpty(mediaID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Media ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      return;
    }

    this.zoneID = serverParameters.getString(MaioAdsManager.KEY_ZONE_ID);
    if (TextUtils.isEmpty(zoneID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid Zone ID.",
          ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
      return;
    }

    MaioAds.setAdTestMode(mediationAdRequest.isTesting());
    MaioAdsManager.getManager(mediaID)
        .initialize(
            (Activity) context,
            new MaioAdsManager.InitializationListener() {
              @Override
              public void onMaioInitialized() {
                MaioAdsManager.getManager(mediaID).loadAd(zoneID, Interstitial.this);
              }
            });
  }

  @Override
  public void showInterstitial() {
    MaioAdsManager.getManager(mediaID).showAd(zoneID, Interstitial.this);
  }
  // endregion

  // region MaioAdsManagerListener implementation
  @Override
  public void onChangedCanShow(String zoneId, boolean isAvailable) {
    if (this.mediationInterstitialListener != null && isAvailable) {
      this.mediationInterstitialListener.onAdLoaded(Interstitial.this);
    }
  }

  @Override
  public void onFailed(FailNotificationReason reason, String zoneId) {
    AdError error = MaioMediationAdapter.getAdError(reason);
    Log.w(TAG, error.getMessage());
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
    }
  }

  @Override
  public void onAdFailedToShow(@NonNull AdError error) {
    Log.w(TAG, error.getMessage());
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdOpened(Interstitial.this);
      this.mediationInterstitialListener.onAdClosed(Interstitial.this);
    }
  }

  @Override
  public void onAdFailedToLoad(@NonNull AdError error) {
    Log.w(TAG, error.getMessage());
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
    }
  }

  @Override
  public void onOpenAd(String zoneId) {
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdOpened(Interstitial.this);
    }
  }

  @Override
  public void onStartedAd(String zoneId) {
    // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
  }

  @Override
  public void onClickedAd(String zoneId) {
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdClicked(Interstitial.this);
      this.mediationInterstitialListener.onAdLeftApplication(Interstitial.this);
    }
  }

  @Override
  public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
    // No relevant Interstitial Ad event to forward to the Google Mobile Ads SDK.
  }

  @Override
  public void onClosedAd(String zoneId) {
    if (this.mediationInterstitialListener != null) {
      this.mediationInterstitialListener.onAdClosed(Interstitial.this);
    }
  }
  // endregion

}
