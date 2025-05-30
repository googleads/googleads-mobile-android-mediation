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

package com.google.ads.mediation.inmobi.renderers;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_DISPLAY_FAILED;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_AD_NOT_READY;
import static com.google.ads.mediation.inmobi.InMobiConstants.WATERMARK_ALPHA;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.InMobiAdFactory;
import com.google.ads.mediation.inmobi.InMobiAdapterUtils;
import com.google.ads.mediation.inmobi.InMobiConstants;
import com.google.ads.mediation.inmobi.InMobiInitializer;
import com.google.ads.mediation.inmobi.InMobiInterstitialWrapper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.WatermarkData;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import java.util.Map;

public abstract class InMobiInterstitialAd extends InterstitialAdEventListener
    implements MediationInterstitialAd {

  private InMobiInterstitialWrapper inMobiInterstitialWrapper;
  protected final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;
  protected final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;
  protected InMobiInitializer inMobiInitializer;
  private MediationInterstitialAdCallback interstitialAdCallback;

  private InMobiAdFactory inMobiAdFactory;

  public InMobiInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback,
      @NonNull InMobiInitializer inMobiInitializer,
      @NonNull InMobiAdFactory inMobiAdFactory) {
    this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiInitializer = inMobiInitializer;
    this.inMobiAdFactory = inMobiAdFactory;
  }

  /** Invokes the third-party method for loading the ad. */
  protected abstract void internalLoadAd(InMobiInterstitialWrapper inMobiInterstitialWrapper);

  public abstract void loadAd();

  protected void createAndLoadInterstitialAd(final Context context, long placementId) {
    inMobiInterstitialWrapper =
        inMobiAdFactory.createInMobiInterstitialWrapper(context, placementId, this);

    // Set the COPPA value in InMobi SDK.
    InMobiAdapterUtils.setIsAgeRestricted();

    InMobiAdapterUtils.configureGlobalTargeting(
        mediationInterstitialAdConfiguration.getMediationExtras());

    String watermark = mediationInterstitialAdConfiguration.getWatermark();
    if (!TextUtils.isEmpty(watermark)) {
      inMobiInterstitialWrapper.setWatermarkData(new WatermarkData(watermark, WATERMARK_ALPHA));
    }

    internalLoadAd(inMobiInterstitialWrapper);
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (!inMobiInterstitialWrapper.isReady()) {
      AdError error = InMobiConstants.createAdapterError(ERROR_AD_NOT_READY,
          "InMobi interstitial ad is not yet ready to be shown.");
      Log.w(TAG, error.toString());
      if (interstitialAdCallback != null) {
        interstitialAdCallback.onAdFailedToShow(error);
      }
      return;
    }

    inMobiInterstitialWrapper.show();
  }

  @Override
  public void onUserLeftApplication(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi interstitial ad has caused the user to leave the application.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onRewardsUnlocked(@NonNull InMobiInterstitial inMobiInterstitial,
      Map<Object, Object> rewards) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdDisplayFailed(@NonNull InMobiInterstitial inMobiInterstitial) {
    AdError error = InMobiConstants.createAdapterError(ERROR_AD_DISPLAY_FAILED,
        "InMobi SDK failed to display an interstitial ad.");
    Log.e(TAG, error.toString());
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdFailedToShow(error);
    }
  }

  @Override
  public void onAdWillDisplay(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi interstitial ad will be shown.");
    // No-op, `onAdDisplayed` will be used to forward the Google Mobile Ads SDK `onAdOpened`
    // callback instead.
  }

  @Override
  public void onAdLoadSucceeded(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi interstitial ad has been loaded.");
    if (mediationAdLoadCallback != null) {
      interstitialAdCallback = mediationAdLoadCallback.onSuccess(InMobiInterstitialAd.this);
    }
  }

  @Override
  public void onAdLoadFailed(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull InMobiAdRequestStatus inMobiAdRequestStatus) {
    AdError error = InMobiConstants.createSdkError(
        InMobiAdapterUtils.getMediationErrorCode(inMobiAdRequestStatus),
        inMobiAdRequestStatus.getMessage());
    Log.e(TAG, error.toString());
    if (mediationAdLoadCallback != null) {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdFetchSuccessful(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi SDK fetched the interstitial ad successfully, but the ad "
        + "contents still need to be loaded.");
  }

  @Override
  public void onAdDisplayed(@NonNull InMobiInterstitial inMobiInterstitial,
      @NonNull AdMetaInfo adMetaInfo) {
    Log.d(TAG, "InMobi interstitial ad has been shown.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdDismissed(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi interstitial ad has been dismissed.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void onAdClicked(@NonNull InMobiInterstitial inMobiInterstitial,
      Map<Object, Object> clickParameters) {
    Log.d(TAG, "InMobi interstitial ad has been clicked.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdImpression(@NonNull InMobiInterstitial inMobiInterstitial) {
    Log.d(TAG, "InMobi interstitial ad has logged an impression.");
    if (interstitialAdCallback != null) {
      interstitialAdCallback.reportAdImpression();
    }
  }
}
