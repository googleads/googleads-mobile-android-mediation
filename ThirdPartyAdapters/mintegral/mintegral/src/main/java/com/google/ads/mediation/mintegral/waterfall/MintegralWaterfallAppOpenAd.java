// Copyright 2023 Google LLC
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

package com.google.ads.mediation.mintegral.waterfall;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralFactory;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralAppOpenAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;

/**
 * Used to show Mintegral splash ads and mediate callbacks between Google Mobile Ads SDK and
 * Mintegral SDK.
 */
public class MintegralWaterfallAppOpenAd extends MintegralAppOpenAd {

  public MintegralWaterfallAppOpenAd(
      @NonNull
          MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> adLoadCallback) {
    super(adLoadCallback);
  }

  @Override
  public void loadAd(MediationAppOpenAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String adUnitId = serverParameters.getString(MintegralConstants.AD_UNIT_ID);
    String placementId = serverParameters.getString(MintegralConstants.PLACEMENT_ID);
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    splashAdWrapper = MintegralFactory.createSplashAdWrapper();
    splashAdWrapper.createAd(placementId, adUnitId);
    splashAdWrapper.setSplashLoadListener(this);
    splashAdWrapper.setSplashShowListener(this);
    splashAdWrapper.preLoad();
  }

  @Override
  public void showAd(@NonNull Context context) {
    // Context passed here is the activity that the publisher passed to GMA SDK's show() method
    // (https://developers.google.com/admob/android/reference/com/google/android/gms/ads/appopen/AppOpenAd#show(android.app.Activity)).
    // So, this will be an activity context.
    Activity activity = (Activity) context;
    if (splashAdWrapper != null) {
      RelativeLayout layout = new RelativeLayout(activity);
      ((ViewGroup) (activity.getWindow().getDecorView().findViewById(android.R.id.content)))
          .addView(layout);
      splashAdWrapper.show(layout);
    }
  }
}
