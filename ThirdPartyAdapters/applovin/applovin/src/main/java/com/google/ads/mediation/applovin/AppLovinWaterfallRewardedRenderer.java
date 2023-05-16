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

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static com.applovin.mediation.ApplovinAdapter.log;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_AD_ALREADY_REQUESTED;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.ERROR_PRESENTATON_AD_NOT_READY;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinUtils;
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

public class AppLovinWaterfallRewardedRenderer
    extends AppLovinRewardedRenderer implements MediationRewardedAd {

  private static final Object INCENTIVIZED_ADS_LOCK = new Object();
  private static final HashMap<String, WeakReference<AppLovinWaterfallRewardedRenderer>>
      INCENTIVIZED_ADS = new HashMap<>();

  /**
   * AppLovin's default zone.
   */
  private static final String DEFAULT_ZONE = "";

  /**
   * AppLovin rewarded ad zone ID.
   */
  private String zoneId;

  protected AppLovinWaterfallRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    super(adConfiguration, callback);
  }

  @Override
  public void loadAd() {
    final Context context = adConfiguration.getContext();
    final Bundle serverParameters = adConfiguration.getServerParameters();
    String sdkKey = AppLovinUtils.retrieveSdkKey(context, serverParameters);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid SDK Key.", ERROR_DOMAIN);
      log(ERROR, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    AppLovinInitializer.getInstance()
        .initialize(
            context,
            sdkKey,
            new OnInitializeSuccessListener() {
              @Override
              public void onInitializeSuccess(@NonNull String sdkKey) {
                zoneId = AppLovinUtils.retrieveZoneId(serverParameters);
                appLovinSdk =
                    AppLovinInitializer.getInstance().retrieveSdk(serverParameters, context);

                String logMessage =
                    String.format("Requesting rewarded video for zone '%s'", zoneId);
                log(DEBUG, logMessage);

                // Check if incentivized ad for zone already exists.
                boolean adAlreadyRequested = false;
                synchronized (INCENTIVIZED_ADS_LOCK) {
                  if (INCENTIVIZED_ADS.containsKey(zoneId)) {
                    adAlreadyRequested = true;
                  } else {
                    INCENTIVIZED_ADS.put(
                        zoneId, new WeakReference<>(AppLovinWaterfallRewardedRenderer.this));
                  }
                }

                if (adAlreadyRequested) {
                  AdError error =
                      new AdError(
                          ERROR_AD_ALREADY_REQUESTED,
                          "Cannot load multiple rewarded ads with the same Zone ID. "
                              + "Display one ad before attempting to load another.",
                          ERROR_DOMAIN);
                  log(ERROR, error.toString());
                  adLoadCallback.onFailure(error);
                  return;
                }

                // If this is a default Zone, create the incentivized ad normally.
                if (Objects.equals(zoneId, DEFAULT_ZONE)) {
                  incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(appLovinSdk);
                } else {
                  // Otherwise, use the Zones API.
                  incentivizedInterstitial =
                      AppLovinIncentivizedInterstitial.create(zoneId, appLovinSdk);
                }
                incentivizedInterstitial.preload(AppLovinWaterfallRewardedRenderer.this);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    appLovinSdk.getSettings()
        .setMuted(AppLovinUtils.shouldMuteAudio(adConfiguration.getMediationExtras()));

    if (zoneId != null) {
      String logMessage = String.format("Showing rewarded video for zone '%s'", zoneId);
      log(DEBUG, logMessage);
    }

    if (!incentivizedInterstitial.isAdReadyToDisplay()) {
      AdError error = new AdError(ERROR_PRESENTATON_AD_NOT_READY, "Ad not ready to show.",
          ERROR_DOMAIN);
      log(ERROR, error.toString());
      rewardedAdCallback.onAdFailedToShow(error);
      return;
    }

    incentivizedInterstitial.show(context, AppLovinWaterfallRewardedRenderer.this,
        AppLovinWaterfallRewardedRenderer.this, AppLovinWaterfallRewardedRenderer.this,
        AppLovinWaterfallRewardedRenderer.this);
  }

  // region AppLovinAdLoadListener implementation
  @Override
  public void failedToReceiveAd(final int code) {
    INCENTIVIZED_ADS.remove(zoneId);
    super.failedToReceiveAd(code);
  }
  // endregion

  // region AppLovinAdDisplayListener implementation.
  @Override
  public void adHidden(@NonNull AppLovinAd ad) {
    INCENTIVIZED_ADS.remove(zoneId);
    super.adHidden(ad);
  }
  // endregion
}
