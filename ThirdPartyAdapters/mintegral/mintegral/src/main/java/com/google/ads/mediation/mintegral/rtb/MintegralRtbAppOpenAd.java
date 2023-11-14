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

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.mbridge.msdk.MBridgeConstans;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serves to show Mintegral splash ads and mediate callbacks between Google Mobile Ads SDK and
 * Mintegral SDK.
 */
public class MintegralRtbAppOpenAd extends MintegralAppOpenAd {

  public MintegralRtbAppOpenAd(
      @NonNull MediationAppOpenAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> callback) {
    super(adConfiguration, callback);
  }

  @Override
  public void loadAd() {
    activity = (Activity) adConfiguration.getContext();
    Bundle serverParameters = adConfiguration.getServerParameters();
    String adUnitId = serverParameters.getString(MintegralConstants.AD_UNIT_ID);
    String placementId = serverParameters.getString(MintegralConstants.PLACEMENT_ID);
    String bidToken = adConfiguration.getBidResponse();
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
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
