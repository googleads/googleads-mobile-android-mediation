// Copyright 2018 Google LLC
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

package com.google.ads.mediation.nend;

import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * The {@link NendExtrasBundleBuilder} class builds a {@link Bundle} containing mediation extras for
 * the nend ad network. The bundles built by this class should be used with {@link
 * com.google.android.gms.ads.AdRequest.Builder#addNetworkExtrasBundle(Class, Bundle)}.
 */
public class NendExtrasBundleBuilder {

  /**
   * The nend User ID.
   */
  private String userId;

  /**
   * Type of interstitial ad to be loaded.
   */
  private NendAdapter.InterstitialType interstitialType;

  /**
   * Type of native ad to be loaded.
   */
  private NendMediationAdapter.FormatType nativeAdsType;

  public NendExtrasBundleBuilder setUserId(@NonNull String userId) {
    this.userId = userId;
    return this;
  }

  public NendExtrasBundleBuilder setInterstitialType(
      NendAdapter.InterstitialType interstitialType) {
    this.interstitialType = interstitialType;
    return this;
  }

  public NendExtrasBundleBuilder setNativeAdsType(NendMediationAdapter.FormatType nativeAdsType) {
    this.nativeAdsType = nativeAdsType;
    return this;
  }

  public Bundle build() {
    Bundle bundle = new Bundle();
    bundle.putString(NendAdapter.KEY_USER_ID, userId);
    bundle.putSerializable(NendAdapter.KEY_INTERSTITIAL_TYPE, interstitialType);
    bundle.putSerializable(NendNativeAdForwarder.KEY_NATIVE_ADS_FORMAT_TYPE, nativeAdsType);
    return bundle;
  }
}
