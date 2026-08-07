// Copyright 2026 Google LLC
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

package com.google.ads.mediation.maio;

import android.content.Context;
import jp.maio.sdk.android.v2.banner.MaioBannerView;
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback;
import jp.maio.sdk.android.v2.interstitial.Interstitial;
import jp.maio.sdk.android.v2.request.MaioRequest;
import jp.maio.sdk.android.v2.rewarded.IRewardedLoadCallback;
import jp.maio.sdk.android.v2.rewarded.Rewarded;

/** Helper methods in Java to invoke Java static methods and method overloads for Kotlin tests. */
public final class MaioTestHelper {
  private MaioTestHelper() {}

  public static Interstitial loadInterstitialAd(
      MaioRequest request, Context context, IInterstitialLoadCallback callback) {
    return Interstitial.loadAd(request, context, callback);
  }

  public static Rewarded loadRewardedAd(
      MaioRequest request, Context context, IRewardedLoadCallback callback) {
    return Rewarded.loadAd(request, context, callback);
  }

  public static void loadBannerView(MaioBannerView bannerView, boolean isTesting) {
    bannerView.load(isTesting);
  }
}
