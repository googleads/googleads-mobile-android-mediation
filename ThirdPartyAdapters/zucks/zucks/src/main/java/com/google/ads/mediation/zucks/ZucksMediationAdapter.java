package com.google.ads.mediation.zucks;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.util.List;

/**
 * Mediation Adapter for Zucks Ad Network.
 * <p>
 * Supported formats:
 *   - Rewarded Ad (In the plan)
 * <p>
 * Unsupported formats:
 *   - Banner
 *   - Interstitial
 * <p>
 * For compatibility, Banner/Interstitial Ad is implemented only in legacy adapter's FQCN.
 * If you want to integrate Banner/Interstitial Ad, see the legacy adapter implementation.
 *
 * @see com.google.android.gms.ads.mediation.ZucksAdapter ZucksAdapter
 */
public class ZucksMediationAdapter extends Adapter {

  /** Interstitial types for Zucks Ad Network SDK. */
  public enum InterstitialType {
    /**
     * @see <a
     *     href="https://ms.zucksadnetwork.com/media/sdk/manual/android/#adFullscreenInterstitial">ⅳ.
     *     全画面（縦）インタースティシャル広告 - Zucks Ad Network Android SDK 導入手順</a>
     */
    FULLSCREEN,
    /**
     * @see <a href="https://ms.zucksadnetwork.com/media/sdk/manual/android/#adInterstitial">ⅱ.
     *     インタースティシャル広告 - Zucks Ad Network Android SDK 導入手順</a>
     */
    MEDIUM_RECTANGLE,
  }

  @Override
  public void initialize(
          Context context,
          InitializationCompleteCallback initializationCompleteCallback,
          List<MediationConfiguration> list) {
    // Initialization is not needed in Zucks Ad Network SDK.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public VersionInfo getVersionInfo() {
    return AdMobUtil.getAdapterVersionInfo();
  }

  @Override
  public VersionInfo getSDKVersionInfo() {
    return AdMobUtil.getNetworkSdkVersionInfo();
  }

  /** Format-wide extras builder. */
  public static class MediationExtrasBundleBuilder {

    /** Default value of setInterstitialType. */
    public static final InterstitialType DEFAULT_INTERSTITIAL_TYPE =
        InterstitialType.MEDIUM_RECTANGLE;

    public static final String KEY_FULLSCREEN_FOR_INTERSTITIAL = "fullscreen";

    @NonNull private InterstitialType interstitialType = DEFAULT_INTERSTITIAL_TYPE;

    /** Switch interstitial format. */
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
