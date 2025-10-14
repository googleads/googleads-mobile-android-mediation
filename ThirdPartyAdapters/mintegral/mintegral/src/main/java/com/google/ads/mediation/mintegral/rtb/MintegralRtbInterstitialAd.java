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

package com.google.ads.mediation.mintegral.rtb;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.loadedSlotIdentifiers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.FlagValueGetter;
import com.google.ads.mediation.mintegral.MintegralBidNewInterstitialAdWrapper;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralFactory;
import com.google.ads.mediation.mintegral.MintegralSlotIdentifier;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import java.lang.ref.WeakReference;
import org.json.JSONException;
import org.json.JSONObject;

public class MintegralRtbInterstitialAd extends MintegralInterstitialAd {

  private MintegralBidNewInterstitialAdWrapper mbBidNewInterstitialAdWrapper;

  public MintegralRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration adConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              callback,
      FlagValueGetter flagValueGetter) {
    super(adConfiguration, callback, flagValueGetter);
  }

  @Override
  public void loadAd(MediationInterstitialAdConfiguration adConfiguration) {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    String bidToken = adConfiguration.getBidResponse();
    AdError error = MintegralUtils.validateMintegralAdLoadParams(adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }

    if (flagValueGetter.shouldRestrictMultipleAdLoads()) {
      mintegralSlotIdentifier = new MintegralSlotIdentifier(adUnitId, placementId);
      loadedSlotIdentifiers.put(mintegralSlotIdentifier, new WeakReference<>(this));
    }

    mbBidNewInterstitialAdWrapper = MintegralFactory.createBidInterstitialHandler();
    mbBidNewInterstitialAdWrapper.createAd(adConfiguration.getContext(), placementId, adUnitId);
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MBridgeConstans.EXTRA_KEY_WM, adConfiguration.getWatermark());
      mbBidNewInterstitialAdWrapper.setExtraInfo(jsonObject);
    } catch (JSONException jsonException) {
      Log.w(TAG, "Failed to apply watermark to Mintegral bidding interstitial ad.",
          jsonException);
    }
    mbBidNewInterstitialAdWrapper.setInterstitialVideoListener(this);
    mbBidNewInterstitialAdWrapper.loadFromBid(bidToken);
  }

  @Override
  public void showAd(@NonNull Context context) {
    mbBidNewInterstitialAdWrapper.playVideoMute(
        muted
            ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE
            : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);
    mbBidNewInterstitialAdWrapper.showFromBid();
  }
}
