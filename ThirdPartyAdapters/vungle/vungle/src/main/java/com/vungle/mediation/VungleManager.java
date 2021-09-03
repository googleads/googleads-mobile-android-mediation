package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.vungle.warren.Vungle;
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

  synchronized boolean canRequestBannerAd(@NonNull String placementId,
      @Nullable String requestUniqueId) {
    cleanLeakedBannerAdapters();

    VungleBannerAd bannerAd = mVungleBanners.get(placementId);
    if (bannerAd == null) {
      return true;
    }

    if (bannerAd.getAdapter() == null) {
      mVungleBanners.remove(placementId);
      return true;
    }

    VungleBannerAdapter adapter = bannerAd.getAdapter();
    String activeUniqueRequestId = adapter.getUniqueRequestId();
    Log.d(TAG,
        "activeUniqueId: " + activeUniqueRequestId + " ###  RequestId: " + requestUniqueId);

    if (activeUniqueRequestId == null) {
      Log.w(TAG, "Ad already loaded for placement ID: " + placementId + ", and cannot "
          + "determine if this is a refresh. Set Vungle extras when making an ad request to "
          + "support refresh on Vungle banner ads.");
      return false;
    }

    if (!activeUniqueRequestId.equals(requestUniqueId)) {
      Log.w(TAG, "Ad already loaded for placement ID: " + placementId);
      return false;
    }

    return true;
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
}