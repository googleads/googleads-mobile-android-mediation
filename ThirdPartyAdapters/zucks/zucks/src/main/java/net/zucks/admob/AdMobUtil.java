package net.zucks.admob;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.ZucksAdapter;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import net.zucks.BuildConfig;
import net.zucks.internal.common.Platform;
import net.zucks.util.ZucksLog;
import net.zucks.view.AdBanner;
import net.zucks.view.IZucksInterstitial;

/** Format-wide logic and constants. */
public final class AdMobUtil {

  /** Get (this) adapter's package name. */
  @NonNull
  public static final String ADAPTER_DOMAIN =
      com.google.ads.mediation.zucks.BuildConfig.LIBRARY_PACKAGE_NAME;

  /** Get Zucks Ad Network SDK's package name (from its module). */
  @NonNull public static final String SDK_DOMAIN = net.zucks.BuildConfig.LIBRARY_PACKAGE_NAME;

  @NonNull public static final ZucksLog ZUCKS_LOG = new ZucksLog(ZucksAdapter.class);

  /** Frame ID key in serverParams. */
  @NonNull private static final String AD_FRAME_ID = "frame_id";

  /** Version object of Zucks Ad Network SDK. */
  @NonNull
  public static VersionInfo getNetworkSdkVersionInfo() {
    return new VersionInfo(
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_MAJOR),
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_MINOR),
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_SDK_PATCH));
  }

  /** Version string of Zucks Ad Network SDK. */
  @NonNull
  public static String getNetworkSdkVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  /**
   * Version object of (this) mediation adapter.
   *
   * <p>`VersionInfo` does not support the 4-segment versioning. This method will be joining sdk and
   * adapter's patch segment.
   */
  @NonNull
  public static VersionInfo getAdapterVersionInfo() {
    int sdkPatch =
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_SDK_PATCH)
            * 100;

    int adapterPatch =
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_ADAPTER_PATCH);

    return new VersionInfo(
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_MAJOR),
        Integer.parseInt(com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION_MINOR),
        sdkPatch + adapterPatch);
  }

  /** Version string of (this) mediation adapter. */
  @NonNull
  public static String getAdapterVersionName() {
    return com.google.ads.mediation.zucks.BuildConfig.ADAPTER_VERSION;
  }

  /** Get FrameID from serverParams. */
  @Nullable
  public static String getFrameId(@Nullable Bundle serverParams) {
    if (serverParams != null && serverParams.containsKey(AD_FRAME_ID)) {
      return serverParams.getString(AD_FRAME_ID);
    }
    return null;
  }

  // region configurePlatform
  /** Configure mediation platform flags for Banner. */
  public static void configurePlatform(@NonNull AdBanner adBanner) {
    adBanner.setPlatform(
        Platform.ADMOB, getGooglePlayServicesVersionCode(), AdMobUtil.getAdapterVersionName());
  }

  /** Configure mediation platform flags for Interstitial. */
  public static void configurePlatform(@NonNull IZucksInterstitial zucksInterstitial) {
    zucksInterstitial.setPlatform(
        Platform.ADMOB, getGooglePlayServicesVersionCode(), AdMobUtil.getAdapterVersionName());
  }
  // endregion

  private static String getGooglePlayServicesVersionCode() {
    return String.valueOf(GoogleApiAvailabilityLight.GOOGLE_PLAY_SERVICES_VERSION_CODE);
  }
}
