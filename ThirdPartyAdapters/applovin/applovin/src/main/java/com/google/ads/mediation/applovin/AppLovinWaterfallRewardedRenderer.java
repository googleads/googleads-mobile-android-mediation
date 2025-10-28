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

package com.google.ads.mediation.applovin;

import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MISSING_SDK_KEY;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_MSG_MISSING_SDK;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_PRESENTATION_AD_NOT_READY;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.sdk.AppLovinAd;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

/**
 * The {@link AppLovinWaterfallRewardedRenderer} is used to load AppLovin Incentivized Interstitial
 * or Rewarded ads and mediate the callbacks between Google Mobile Ads SDK and Unity Ads SDK.
 */
public class AppLovinWaterfallRewardedRenderer extends AppLovinRewardedRenderer
    implements MediationRewardedAd {

  @VisibleForTesting
  protected static final HashMap<String, WeakReference<AppLovinWaterfallRewardedRenderer>>
      incentivizedAdsMap = new HashMap<>();

  /** AppLovin's default zone. */
  private static final String DEFAULT_ZONE = "";

  /** AppLovin rewarded ad zone ID. */
  private String zoneId;

  // Flag to let multiple loading of ads
  private boolean enableMultipleAdLoading = false;

  protected AppLovinWaterfallRewardedRenderer(
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback,
      @NonNull AppLovinInitializer appLovinInitializer,
      @NonNull AppLovinAdFactory appLovinAdFactory,
      @NonNull AppLovinSdkUtilsWrapper appLovinSdkUtilsWrapper) {
    super(callback, appLovinInitializer, appLovinAdFactory, appLovinSdkUtilsWrapper);
  }

  @Override
  public void loadAd(@NonNull MediationRewardedAdConfiguration adConfiguration) {
    final Context context = adConfiguration.getContext();
    final Bundle serverParameters = adConfiguration.getServerParameters();
    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error =
          new AdError(ERROR_MISSING_SDK_KEY, ERROR_MSG_MISSING_SDK, APPLOVIN_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    if (AppLovinUtils.isMultiAdsEnabled()) {
      enableMultipleAdLoading = true;
    }
    networkExtras = adConfiguration.getMediationExtras();

    appLovinInitializer.initialize(
        context,
        sdkKey,
        new OnInitializeSuccessListener() {
          @Override
          public void onInitializeSuccess() {
            zoneId = AppLovinUtils.retrieveZoneId(serverParameters);
            appLovinSdk = appLovinInitializer.retrieveSdk(context);

            String logMessage = String.format("Requesting rewarded video for zone '%s'", zoneId);
            Log.d(TAG, logMessage);

            // Check if incentivized ad for zone already exists.
            boolean adAlreadyRequested = false;
            if (incentivizedAdsMap.containsKey(zoneId)) {
              adAlreadyRequested = true;
            } else {
              incentivizedAdsMap.put(
                  zoneId, new WeakReference<>(AppLovinWaterfallRewardedRenderer.this));
            }

            if (adAlreadyRequested) {
              AdError error =
                  new AdError(
                      ERROR_AD_ALREADY_REQUESTED, ERROR_MSG_MULTIPLE_REWARDED_AD, ERROR_DOMAIN);
              Log.e(TAG, error.toString());
              adLoadCallback.onFailure(error);
              return;
            }

            // If this is a default Zone, create the incentivized ad normally.
            if (Objects.equals(zoneId, DEFAULT_ZONE)) {
              incentivizedInterstitial =
                  appLovinAdFactory.createIncentivizedInterstitial(appLovinSdk);
            } else {
              // Otherwise, use the Zones API.
              incentivizedInterstitial =
                  appLovinAdFactory.createIncentivizedInterstitial(zoneId, appLovinSdk);
            }
            incentivizedInterstitial.preload(AppLovinWaterfallRewardedRenderer.this);
          }
        });
  }

  @Override
  public void showAd(@NonNull Context context) {
    appLovinSdk.getSettings().setMuted(AppLovinUtils.shouldMuteAudio(networkExtras));

    if (zoneId != null) {
      String logMessage = String.format("Showing rewarded video for zone '%s'", zoneId);
      Log.d(TAG, logMessage);
    }

    if (!incentivizedInterstitial.isAdReadyToDisplay()) {
      AdError error =
          new AdError(ERROR_PRESENTATION_AD_NOT_READY, ERROR_MSG_AD_NOT_READY, ERROR_DOMAIN);
      Log.e(TAG, error.toString());
      rewardedAdCallback.onAdFailedToShow(error);
      return;
    }

    incentivizedInterstitial.show(context, this, this, this, this);
  }

  @Override
  public void adReceived(@NonNull AppLovinAd appLovinAd) {
    if (enableMultipleAdLoading) {
      incentivizedAdsMap.remove(zoneId);
    }
    super.adReceived(appLovinAd);
  }

  // region AppLovinAdLoadListener implementation
  @Override
  public void failedToReceiveAd(final int code) {
    incentivizedAdsMap.remove(zoneId);
    super.failedToReceiveAd(code);
  }

  // endregion

  // region AppLovinAdDisplayListener implementation.
  @Override
  public void adHidden(@NonNull AppLovinAd ad) {
    incentivizedAdsMap.remove(zoneId);
    super.adHidden(ad);
  }
  // endregion
}
