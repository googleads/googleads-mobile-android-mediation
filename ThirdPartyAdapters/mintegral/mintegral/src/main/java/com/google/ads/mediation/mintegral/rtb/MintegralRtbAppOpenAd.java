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

package com.google.ads.mediation.mintegral.rtb;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.loadedSlotIdentifiers;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.FlagValueGetter;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralFactory;
import com.google.ads.mediation.mintegral.MintegralSlotIdentifier;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralAppOpenAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAd;
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback;
import com.google.android.gms.ads.mediation.MediationAppOpenAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import java.lang.ref.WeakReference;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serves to show Mintegral splash ads and mediate callbacks between Google Mobile Ads SDK and
 * Mintegral SDK.
 */
public class MintegralRtbAppOpenAd extends MintegralAppOpenAd {

  private String bidToken;

  public MintegralRtbAppOpenAd(
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback,
      FlagValueGetter flagValueGetter) {
    super(callback, flagValueGetter);
  }

  @Override
  public void loadAd(MediationAppOpenAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String adUnitId = serverParameters.getString(MintegralConstants.AD_UNIT_ID);
    String placementId = serverParameters.getString(MintegralConstants.PLACEMENT_ID);
    bidToken = adConfiguration.getBidResponse();
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }

    if (flagValueGetter.shouldRestrictMultipleAdLoads()) {
      mintegralSlotIdentifier = new MintegralSlotIdentifier(adUnitId, placementId);
      loadedSlotIdentifiers.put(mintegralSlotIdentifier, new WeakReference<>(this));
    }

    splashAdWrapper = MintegralFactory.createSplashAdWrapper();
    String watermark = adConfiguration.getWatermark();
    if(!TextUtils.isEmpty(watermark)) {
      setWatermark(watermark);
    }
    splashAdWrapper.createAd(placementId, adUnitId);
    splashAdWrapper.setSplashLoadListener(this);
    splashAdWrapper.setSplashShowListener(this);
    splashAdWrapper.preLoadByToken(bidToken);
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
      splashAdWrapper.show(layout, bidToken);
    }
  }

  private void setWatermark(String watermark) {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MBridgeConstans.EXTRA_KEY_WM, watermark);
      splashAdWrapper.setExtraInfo(jsonObject);
    } catch (JSONException jsonException) {
      Log.w(TAG, "Failed to apply watermark to Mintegral bidding app open ad.", jsonException);
    }
  }
}
