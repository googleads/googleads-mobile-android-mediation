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

package com.applovin.mediation;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static com.applovin.mediation.AppLovinUtils.getChildUserError;
import static com.applovin.mediation.AppLovinUtils.isChildUser;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.applovin.adview.AppLovinAdView;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.google.ads.mediation.applovin.AppLovinInitializer;
import com.google.ads.mediation.applovin.AppLovinInitializer.OnInitializeSuccessListener;
import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;

/**
 * The {@link ApplovinAdapter} class is used to load AppLovin Banner, interstitial & rewarded-based
 * video ads and to mediate the callbacks between the AppLovin SDK and the Google Mobile Ads SDK.
 */
public class ApplovinAdapter extends AppLovinMediationAdapter implements MediationBannerAdapter {

  private static final boolean LOGGING_ENABLED = true;

  // Parent objects.
  private AppLovinSdk sdk;

  // Banner objects.
  private FrameLayout adViewWrapper;
  private AppLovinAdView adView;

  // Controlled fields.
  private String zoneId;

  // region MediationBannerAdapter implementation.
  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener mediationBannerListener,
      @NonNull final Bundle serverParameters, @NonNull final AdSize adSize,
      @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle networkExtras) {
    if (isChildUser()) {
      mediationBannerListener.onAdFailedToLoad(this, getChildUserError());
      return;
    }

    String sdkKey = serverParameters.getString(ServerParameterKeys.SDK_KEY);
    if (TextUtils.isEmpty(sdkKey)) {
      AdError error =
          new AdError(ERROR_MISSING_SDK_KEY, "Missing or invalid SDK Key.", ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, error);
      return;
    }

    // Convert requested size to AppLovin Ad Size.
    final AppLovinAdSize appLovinAdSize =
        AppLovinUtils.appLovinAdSizeFromAdMobAdSize(context, adSize);
    if (appLovinAdSize == null) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          "Failed to request banner with unsupported size.", ERROR_DOMAIN);
      log(ERROR, error.getMessage());
      mediationBannerListener.onAdFailedToLoad(ApplovinAdapter.this, error);
      return;
    }

    FrameLayout.LayoutParams adViewLayoutParams =
        new FrameLayout.LayoutParams(
            adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
    adViewWrapper = new FrameLayout(context);
    adViewWrapper.setLayoutParams(adViewLayoutParams);

    AppLovinInitializer.getInstance()
        .initialize(
            context,
            sdkKey,
            new OnInitializeSuccessListener() {
              @Override
              public void onInitializeSuccess() {
                // Store parent objects
                sdk = AppLovinInitializer.getInstance().retrieveSdk(context);
                zoneId = AppLovinUtils.retrieveZoneId(serverParameters);

                log(DEBUG, "Requesting banner of size " + appLovinAdSize + " for zone: " + zoneId);
                adView = new AppLovinAdView(sdk, appLovinAdSize, context);

                final AppLovinBannerAdListener listener =
                    new AppLovinBannerAdListener(
                        zoneId, adView, ApplovinAdapter.this, mediationBannerListener);
                adView.setAdDisplayListener(listener);
                adView.setAdClickListener(listener);
                adView.setAdViewEventListener(listener);
                adViewWrapper.addView(adView);

                if (!TextUtils.isEmpty(zoneId)) {
                  sdk.getAdService().loadNextAdForZoneId(zoneId, listener);
                } else {
                  sdk.getAdService().loadNextAd(appLovinAdSize, listener);
                }
              }
            });
  }

  @NonNull
  @Override
  public View getBannerView() {
    return adViewWrapper;
  }
  // endregion

  // region MediationAdapter.
  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  @Override
  public void onDestroy() {
  }
  // endregion

  // Logging
  public static void log(int priority, final String message) {
    if (LOGGING_ENABLED) {
      Log.println(priority, "AppLovinAdapter", message);
    }
  }

}
