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

import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_UNSUPPORTED;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralFactory;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.BannerSize;
import org.json.JSONException;
import org.json.JSONObject;

public class MintegralRtbBannerAd extends MintegralBannerAd {

  public MintegralRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    super(mediationBannerAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void loadAd() {
    BannerSize bannerSize =
        getMintegralBannerSizeFromAdMobAdSize(
            adConfiguration.getAdSize(), adConfiguration.getContext(), /* isRtb= */ true);
    if (bannerSize == null) {
      AdError bannerSizeError = MintegralConstants.createAdapterError(ERROR_BANNER_SIZE_UNSUPPORTED,
          String.format("The requested banner size: %s is not supported by Mintegral SDK.",
              adConfiguration.getAdSize()));
      Log.e(TAG, bannerSizeError.toString());
      adLoadCallback.onFailure(bannerSizeError);
      return;
    }

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
    mbBannerView = MintegralFactory.createMBBannerView(adConfiguration.getContext());
    mbBannerView.init(bannerSize, placementId, adUnitId);
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(MBridgeConstans.EXTRA_KEY_WM, adConfiguration.getWatermark());
      mbBannerView.setExtraInfo(jsonObject);
    } catch (JSONException jsonException) {
      Log.w(TAG, "Failed to apply watermark to Mintegral bidding banner ad.", jsonException);
    }
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
        MintegralUtils.convertDipToPixel(adConfiguration.getContext(), bannerSize.getWidth()),
        MintegralUtils.convertDipToPixel(adConfiguration.getContext(), bannerSize.getHeight()));
    mbBannerView.setLayoutParams(layoutParams);
    mbBannerView.setBannerAdListener(this);
    mbBannerView.loadFromBid(bidToken);
  }
}
