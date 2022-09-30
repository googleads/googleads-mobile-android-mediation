package com.vungle.mediation;

import androidx.annotation.NonNull;
import com.vungle.ads.VungleSettings;

/**
 * To apply the Vungle network settings during initialization.
 */
public class VungleNetworkSettings {

  private static boolean androidIdOptedOut;

  private static VungleSettings vungleSettings;

  public static void setAndroidIdOptOut(boolean isOptedOut) {
    androidIdOptedOut = isOptedOut;
    applySettings();
  }

  /**
   * To pass Vungle network setting to SDK. this method must be called before first loadAd. if
   * called after first loading an ad, settings will not be applied.
   */
  private static void applySettings() {
    vungleSettings = new VungleSettings(androidIdOptedOut);
  }

  @NonNull
  public static VungleSettings getVungleSettings() {
    if (vungleSettings == null) {
      vungleSettings = new VungleSettings(androidIdOptedOut);
    }
    return vungleSettings;
  }

}
