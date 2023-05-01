// Copyright 2017 Google LLC
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

package com.vungle.mediation;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.vungle.warren.AdConfig;
import java.util.UUID;

/**
 * A helper class for creating a network extras bundle that can be passed to the adapter to make
 * customizations specific to Liftoff Monetize.
 */
public final class VungleExtrasBuilder {

  public static final String EXTRA_USER_ID = "userId";
  private static final String EXTRA_START_MUTED = "startMuted";
  private static final String EXTRA_ORDINAL_VIEW_COUNT = "ordinalViewCount";
  private static final String EXTRA_ORIENTATION = "adOrientation";
  static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
  static final String EXTRA_PLAY_PLACEMENT = "playPlacement";
  static final String UUID_KEY = "uniqueVungleRequestKey";

  private final Bundle bundle = new Bundle();

  public VungleExtrasBuilder(@Nullable @Size(min = 1L) String[] placements) {
    bundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
  }

  public VungleExtrasBuilder setPlayingPlacement(String placement) {
    bundle.putString(EXTRA_PLAY_PLACEMENT, placement);
    return this;
  }

  @Deprecated
  public VungleExtrasBuilder setSoundEnabled(boolean enabled) {
    return setStartMuted(!enabled);
  }

  public VungleExtrasBuilder setStartMuted(boolean muted) {
    bundle.putBoolean(EXTRA_START_MUTED, muted);
    return this;
  }

  public VungleExtrasBuilder setUserId(String userId) {
    bundle.putString(EXTRA_USER_ID, userId);
    return this;
  }

  public VungleExtrasBuilder setOrdinalViewCount(int ordinalViewCount) {
    bundle.putInt(EXTRA_ORDINAL_VIEW_COUNT, ordinalViewCount);
    return this;
  }

  public VungleExtrasBuilder setAdOrientation(int adOrientation) {
    bundle.putInt(EXTRA_ORIENTATION, adOrientation);
    return this;
  }

  public VungleExtrasBuilder setBannerUniqueRequestID(String uniqueID) {
    bundle.putString(UUID_KEY, uniqueID);
    return this;
  }

  public Bundle build() {
    if (TextUtils.isEmpty(bundle.getString(UUID_KEY, null))) {
      bundle.putString(UUID_KEY, UUID.randomUUID().toString());
    }
    return bundle;
  }

  public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras, boolean defaultMuteState) {
    AdConfig adConfig = new AdConfig();
    adConfig.setMuted(defaultMuteState);

    if (networkExtras != null) {
      adConfig.setMuted(networkExtras.getBoolean(EXTRA_START_MUTED, defaultMuteState));
      adConfig.setOrdinal(networkExtras.getInt(EXTRA_ORDINAL_VIEW_COUNT, 0));
      adConfig.setAdOrientation(networkExtras.getInt(EXTRA_ORIENTATION, AdConfig.AUTO_ROTATE));
    }
    return adConfig;
  }

  public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras,
      NativeAdOptions options, boolean defaultMuteState) {
    AdConfig adConfig = adConfigWithNetworkExtras(networkExtras, defaultMuteState);

    int privacyIconPlacement;
    if (options != null) {
      privacyIconPlacement = options.getAdChoicesPlacement();
    } else {
      privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;
    }

    int adOptionsPosition;
    switch (privacyIconPlacement) {
      case NativeAdOptions.ADCHOICES_TOP_LEFT:
        adOptionsPosition = AdConfig.TOP_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
        adOptionsPosition = AdConfig.BOTTOM_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
        adOptionsPosition = AdConfig.BOTTOM_RIGHT;
        break;
      case NativeAdOptions.ADCHOICES_TOP_RIGHT:
      default:
        adOptionsPosition = AdConfig.TOP_RIGHT;
        break;
    }

    adConfig.setAdOptionsPosition(adOptionsPosition);

    return adConfig;
  }
}
