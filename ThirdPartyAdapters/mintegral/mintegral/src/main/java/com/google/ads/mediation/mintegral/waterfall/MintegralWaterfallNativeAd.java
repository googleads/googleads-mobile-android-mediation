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

import static com.mbridge.msdk.MBridgeConstans.NATIVE_VIDEO_SUPPORT;

import android.view.View;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.MBNativeHandler;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MintegralWaterfallNativeAd extends MintegralNativeAd {

  private MBNativeHandler mbNativeHandler;

  public MintegralWaterfallNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          mediationAdLoadCallback) {
    super(mediationNativeAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    Map<String, Object> nativeProperties = MBBidNativeHandler.getNativeProperties(placementId,
        adUnitId);
    // Configure the properties of the Mintegral native ad, where video ad will be supported and
    // only one ad will be returned in each ad request.
    nativeProperties.put(NATIVE_VIDEO_SUPPORT, true);
    nativeProperties.put(MBridgeConstans.PROPERTIES_AD_NUM, 1);
    mbNativeHandler = new MBNativeHandler(nativeProperties, adConfiguration.getContext());
    mbNativeHandler.setAdListener(mintegralNativeAdListener);
    mbNativeHandler.load();
  }

  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> map1) {
    // Set click interaction.
    HashMap<String, View> copyClickableAssetViews = new HashMap<>(clickableAssetViews);

    // Exclude Mintegral's Privacy Information Icon image and text from click events.
    copyClickableAssetViews.remove(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
    copyClickableAssetViews.remove("3012");

    ArrayList<View> assetViews = new ArrayList<>(copyClickableAssetViews.values());
    if (mbNativeHandler != null) {
      mbNativeHandler.registerView(view, assetViews, campaign);
    }
  }

  @Override
  public void untrackView(View view) {
    if (mbNativeHandler != null) {
      mbNativeHandler.unregisterView(view, traversalView(view), campaign);
    }
  }
}
