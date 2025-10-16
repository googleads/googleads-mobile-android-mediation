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

package com.google.ads.mediation.vungle.waterfall

import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.renderers.VungleBannerAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.vungle.ads.VungleBannerView

/** Loads Waterfall Banner ads. */
class VungleWaterfallBannerAd(
  mediationAdLoadCallback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>,
  vungleFactory: VungleFactory,
) : VungleBannerAd(mediationAdLoadCallback, vungleFactory) {

  override fun loadAd(
    bannerAdView: VungleBannerView,
    mediationBannerAdConfiguration: MediationBannerAdConfiguration,
  ) {
    bannerAdView.load(null)
  }
}
