package com.vungle.mediation;

import static com.vungle.warren.error.VungleException.AD_EXPIRED;
import static com.vungle.warren.error.VungleException.AD_FAILED_TO_DOWNLOAD;
import static com.vungle.warren.error.VungleException.AD_PAST_EXPIRATION;
import static com.vungle.warren.error.VungleException.ASSET_DOWNLOAD_ERROR;
import static com.vungle.warren.error.VungleException.INVALID_SIZE;
import static com.vungle.warren.error.VungleException.MISSING_REQUIRED_ARGUMENTS_FOR_INIT;
import static com.vungle.warren.error.VungleException.NETWORK_ERROR;
import static com.vungle.warren.error.VungleException.NETWORK_UNREACHABLE;
import static com.vungle.warren.error.VungleException.NO_SERVE;
import static com.vungle.warren.error.VungleException.PLACEMENT_NOT_FOUND;
import static com.vungle.warren.error.VungleException.VUNGLE_NOT_INTIALIZED;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to load and show Vungle ads and keep track of multiple {@link
 * VungleInterstitialAdapter} instances.
 */
public class VungleManager {

  private static final String TAG = VungleManager.class.getSimpleName();
  private static final String PLAYING_PLACEMENT = "placementID";

  private static VungleManager sInstance;

  private final ConcurrentHashMap<String, VungleBannerAd> mVungleBanners;

  public static synchronized VungleManager getInstance() {
    if (sInstance == null) {
      sInstance = new VungleManager();
    }
    return sInstance;
  }

  private VungleManager() {
    mVungleBanners = new ConcurrentHashMap<>();
  }

  @Nullable
  public String findPlacement(Bundle networkExtras, Bundle serverParameters) {
    String placement = null;
    if (networkExtras != null
        && networkExtras.containsKey(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT)) {
      placement = networkExtras.getString(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT);
    }
    if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
      if (placement != null) {
        Log.i(
            TAG,
            "'placementID' had a value in both serverParameters and networkExtras. "
                + "Used one from serverParameters");
      }
      placement = serverParameters.getString(PLAYING_PLACEMENT);
    }
    if (placement == null) {
      Log.e(TAG, "placementID not provided from serverParameters.");
    }
    return placement;
  }

  /**
   * Checks and returns if the passed Placement ID is a valid placement for App ID
   *
   * @param placementId placement identifier
   */
  boolean isValidPlacement(String placementId) {
    return Vungle.isInitialized() && Vungle.getValidPlacements().contains(placementId);
  }

  /**
   * Workaround to finish and clean {@link VungleBannerAdapter} if {@link
   * VungleInterstitialAdapter#onDestroy()} is not called and adapter was garbage collected.
   */
  private void cleanLeakedBannerAdapters() {
    for (String placementId : new HashSet<>(mVungleBanners.keySet())) {
      VungleBannerAd bannerAd = mVungleBanners.get(placementId);
      if (bannerAd != null && bannerAd.getAdapter() == null) {
        removeActiveBannerAd(placementId, bannerAd);
      }
    }
  }

  synchronized AdError canRequestBannerAd(@NonNull String placementId,
      @Nullable String requestUniqueId) {
    cleanLeakedBannerAdapters();

    VungleBannerAd bannerAd = mVungleBanners.get(placementId);
    if (bannerAd == null) {
      return null;
    }

    if (bannerAd.getAdapter() == null) {
      mVungleBanners.remove(placementId);
      return null;
    }

    VungleBannerAdapter adapter = bannerAd.getAdapter();
    String activeUniqueRequestId = adapter.getUniqueRequestId();
    Log.d(TAG,
        "activeUniqueId: " + activeUniqueRequestId + " ###  RequestId: " + requestUniqueId);

    if (activeUniqueRequestId == null) {
      String message = "Ad already loaded for placement ID: " + placementId + ", and cannot "
          + "determine if this is a refresh. Set Vungle extras when making an ad request to "
          + "support refresh on Vungle banner ads.";
      Log.w(TAG, message);
      return new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
    }

    if (!activeUniqueRequestId.equals(requestUniqueId)) {
      String message = "Ad already loaded for placement ID: " + placementId;
      Log.w(TAG, message);
      return new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message, TAG);
    }

    return null;
  }

  public void removeActiveBannerAd(@NonNull String placementId,
      @Nullable VungleBannerAd activeBannerAd) {
    Log.d(TAG, "try to removeActiveBannerAd: " + placementId);

    boolean didRemove = mVungleBanners.remove(placementId, activeBannerAd);
    if (didRemove && activeBannerAd != null) {
      Log.d(TAG, "removeActiveBannerAd: " + activeBannerAd + "; size=" + mVungleBanners.size());
      activeBannerAd.detach();
      activeBannerAd.destroyAd();
    }
  }

  void registerBannerAd(@NonNull String placementId, @NonNull VungleBannerAd instance) {
    removeActiveBannerAd(placementId, mVungleBanners.get(placementId));
    if (!mVungleBanners.containsKey(placementId)) {
      mVungleBanners.put(placementId, instance);
      Log.d(TAG, "registerBannerAd: " + instance + "; size=" + mVungleBanners.size());
    }
  }

  @Nullable
  public VungleBannerAd getVungleBannerAd(@NonNull String placementId) {
    return mVungleBanners.get(placementId);
  }

  @NonNull
  public static AdError mapErrorCode(@NonNull VungleException vungleError,
      @Nullable String domain) {
    switch (vungleError.getExceptionCode()) {
      case INVALID_SIZE:
      case MISSING_REQUIRED_ARGUMENTS_FOR_INIT:
      case PLACEMENT_NOT_FOUND:
      case VUNGLE_NOT_INTIALIZED:
        return new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST,
            "" + vungleError.getLocalizedMessage(), "" + domain);
      case NETWORK_ERROR:
      case NETWORK_UNREACHABLE:
      case ASSET_DOWNLOAD_ERROR:
      case AD_FAILED_TO_DOWNLOAD:
        return new AdError(AdRequest.ERROR_CODE_NETWORK_ERROR,
            "" + vungleError.getLocalizedMessage(), "" + domain);
      case NO_SERVE:
      case AD_EXPIRED:
      case AD_PAST_EXPIRATION:
        return new AdError(AdRequest.ERROR_CODE_NO_FILL,
            "" + vungleError.getLocalizedMessage(), "" + domain);
      default:
        return new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
            "" + vungleError.getLocalizedMessage(), "" + domain);
    }
  }
}
