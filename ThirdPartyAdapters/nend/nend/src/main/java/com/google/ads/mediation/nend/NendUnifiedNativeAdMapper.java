// Copyright 2020 Google LLC
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

import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

abstract class NendUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * Creates a {@link UnifiedNativeAdMapper} for nend Native ad.
   *
   * @param logoImage the logo image. nend's "text-only" native ad format supports a {@code null}
   *                  logo image.
   */
  NendUnifiedNativeAdMapper(@Nullable NendNativeMappedImage logoImage) {
    if (logoImage == null) {
      Log.w(TAG,
          "Missing Icon image of nend's native ad, so UnifiedNativeAd#getIcon() will be null.");
    }
    setIcon(logoImage);
    setOverrideImpressionRecording(true);
  }

  static boolean canDownloadImage(Context context, String url) {
    return context != null && !TextUtils.isEmpty(url);
  }
}
