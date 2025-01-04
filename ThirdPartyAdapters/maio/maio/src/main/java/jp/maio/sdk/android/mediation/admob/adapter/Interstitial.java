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
import com.google.ads.mediation.maio.MaioMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback;
import jp.maio.sdk.android.v2.interstitial.IInterstitialShowCallback;
import jp.maio.sdk.android.v2.request.MaioRequest;

/**
 * maio mediation adapter for AdMob Interstitial videos.
 */
public class Interstitial extends MaioMediationAdapter implements MediationInterstitialAdapter {

  private MediationInterstitialListener mediationInterstitialListener;

  private jp.maio.sdk.android.v2.interstitial.Interstitial maioInterstitial;

  private Context targetContext;

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
    this.targetContext = context;

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

    this.maioInterstitial = jp.maio.sdk.android.v2.interstitial.Interstitial.loadAd(
        new MaioRequest(zoneID, mediationAdRequest.isTesting(), /* bidData= */ ""), context, new IInterstitialLoadCallback() {
          @Override
          public void loaded(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial) {
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdLoaded(Interstitial.this);
            }
          }

          @Override
          public void failed(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial, int errorCode) {
            AdError error = getAdError(errorCode);
            Log.w(TAG, error.getMessage());
            if (mediationInterstitialListener != null) {
              mediationInterstitialListener.onAdFailedToLoad(Interstitial.this, error);
            }
          }
        });
  }

  @Override
  public void showInterstitial() {
    if (this.maioInterstitial != null) {
      this.maioInterstitial.show(this.targetContext, new IInterstitialShowCallback() {
        @Override
        public void opened(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial) {
          if (mediationInterstitialListener != null) {
            mediationInterstitialListener.onAdOpened(Interstitial.this);
          }
        }

        @Override
        public void closed(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial) {
          if (mediationInterstitialListener != null) {
            mediationInterstitialListener.onAdClosed(Interstitial.this);
          }
        }

        @Override
        public void clicked(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial) {
          if (mediationInterstitialListener != null) {
            mediationInterstitialListener.onAdClicked(Interstitial.this);
            mediationInterstitialListener.onAdLeftApplication(Interstitial.this);
          }
        }

        @Override
        public void failed(@NonNull jp.maio.sdk.android.v2.interstitial.Interstitial interstitial, int errorCode) {
          AdError error = getAdError(errorCode);
          Log.w(TAG, error.getMessage());
          if (mediationInterstitialListener != null) {
            mediationInterstitialListener.onAdOpened(Interstitial.this);
            mediationInterstitialListener.onAdClosed(Interstitial.this);
          }
        }
      });
    }
  }
  // endregion
}
