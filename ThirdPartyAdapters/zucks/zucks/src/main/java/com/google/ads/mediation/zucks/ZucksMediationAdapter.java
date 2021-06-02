package com.google.ads.mediation.zucks;

import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * Mediation Adapter for Zucks Ad Network.
 *
 * Supported formats:
 * - Rewarded Ad (In the plan)
 *
 * Unsupported formats:
 * - Banner
 * - Interstitial
 *
 * If you want to integrate Banner/Interstitial Ad, see the legacy  adapter implementation.
 * @see com.google.android.gms.ads.mediation.ZucksAdapter ZucksAdapter
 */
public class ZucksMediationAdapter {

  /**
   * Interstitial types for Zucks Ad Network SDK.
   */
  public enum InterstitialType {
    /**
     * @see <a href="https://ms.zucksadnetwork.com/media/sdk/manual/android/#adFullscreenInterstitial">ⅳ. 全画面（縦）インタースティシャル広告 - Zucks Ad Network Android SDK 導入手順</a>
     */
    FULLSCREEN,
    /**
     * @see <a href="https://ms.zucksadnetwork.com/media/sdk/manual/android/#adInterstitial">ⅱ. インタースティシャル広告 - Zucks Ad Network Android SDK 導入手順</a>
     */
    MEDIUM_RECTANGLE,
  }

  /**
   * Format-wide extras builder.
   */
  public static class MediationExtrasBundleBuilder {

    /**
     * Default value of setInterstitialType.
     */
    public static final InterstitialType DEFAULT_INTERSTITIAL_TYPE =
            InterstitialType.MEDIUM_RECTANGLE;

    public static final String KEY_FULLSCREEN_FOR_INTERSTITIAL = "fullscreen";

    @NonNull
    private InterstitialType interstitialType = DEFAULT_INTERSTITIAL_TYPE;

    /**
     * Switch interstitial format.
     */
    @NonNull
    public MediationExtrasBundleBuilder setInterstitialType(@NonNull InterstitialType type) {
      this.interstitialType = type;
      return this;
    }

    @NonNull
    public Bundle build() {
      Bundle extras = new Bundle();
      extras.putBoolean(
              KEY_FULLSCREEN_FOR_INTERSTITIAL, interstitialType == InterstitialType.FULLSCREEN);
      return extras;
    }
  }

}
