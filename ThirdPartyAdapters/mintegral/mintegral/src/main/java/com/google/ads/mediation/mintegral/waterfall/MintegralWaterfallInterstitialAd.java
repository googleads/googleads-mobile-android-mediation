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

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralFactory;
import com.google.ads.mediation.mintegral.MintegralNewInterstitialAdWrapper;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;

public class MintegralWaterfallInterstitialAd extends MintegralInterstitialAd {

  private MintegralNewInterstitialAdWrapper mbNewInterstitialAdWrapper;

  public MintegralWaterfallInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    super(adConfiguration, callback);
  }

  @Override
  public void loadAd(MediationInterstitialAdConfiguration adConfiguration) {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    mbNewInterstitialAdWrapper = MintegralFactory.createInterstitialHandler();
    mbNewInterstitialAdWrapper.createAd(adConfiguration.getContext(), placementId, adUnitId);
    mbNewInterstitialAdWrapper.setInterstitialVideoListener(this);
    mbNewInterstitialAdWrapper.load();
  }

  @Override
  public void showAd(@NonNull Context context) {
    mbNewInterstitialAdWrapper.playVideoMute(
        muted
            ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE
            : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbNewInterstitialAdWrapper.show();
  }
}
