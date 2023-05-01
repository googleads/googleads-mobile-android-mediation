// Copyright 2019 Google LLC
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

import androidx.annotation.NonNull;
import com.vungle.warren.VungleSettings;

/**
 * To apply the Liftoff Monetize network settings during initialization.
 */
public class VungleNetworkSettings {

  private static final long MEGABYTE = 1024 * 1024;
  private static long minimumSpaceForInit = 50 * MEGABYTE;
  private static long minimumSpaceForAd = 51 * MEGABYTE;
  private static boolean androidIdOptedOut;
  private static VungleSettings vungleSettings;
  private static VungleSettingsChangedListener vungleSettingsChangedListener;

  public static void setMinSpaceForInit(long spaceForInit) {
    minimumSpaceForInit = spaceForInit;
    applySettings();
  }

  public static void setMinSpaceForAdLoad(long spaceForAd) {
    minimumSpaceForAd = spaceForAd;
    applySettings();
  }

  public static void setAndroidIdOptOut(boolean isOptedOut) {
    androidIdOptedOut = isOptedOut;
    applySettings();
  }

  /**
   * To pass Liftoff Monetize network setting to SDK. this method must be called before first
   * loadAd. if called after first loading an ad, settings will not be applied.
   */
  private static void applySettings() {
    vungleSettings =
        new VungleSettings.Builder()
            .setMinimumSpaceForInit(minimumSpaceForInit)
            .setMinimumSpaceForAd(minimumSpaceForAd)
            .setAndroidIdOptOut(androidIdOptedOut)
            .disableBannerRefresh()
            .build();
    if (vungleSettingsChangedListener != null) {
      vungleSettingsChangedListener.onVungleSettingsChanged(vungleSettings);
    }
  }

  @NonNull
  public static VungleSettings getVungleSettings() {
    if (vungleSettings == null) {
      vungleSettings = new VungleSettings.Builder().disableBannerRefresh().build();
    }
    return vungleSettings;
  }

  public static void setVungleSettingsChangedListener(
      VungleSettingsChangedListener settingsChangedListener) {
    vungleSettingsChangedListener = settingsChangedListener;
  }

  public interface VungleSettingsChangedListener {

    void onVungleSettingsChanged(@NonNull VungleSettings vungleSettings);
  }
}