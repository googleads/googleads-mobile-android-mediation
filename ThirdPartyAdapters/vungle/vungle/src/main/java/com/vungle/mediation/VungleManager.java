package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.warren.AdConfig;
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

  private ConcurrentHashMap<String, VungleBannerAdapter> mVungleBanners;

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

  boolean isAdPlayable(String placement) {
    return (placement != null && !placement.isEmpty()) && Vungle.canPlayAd(placement);
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
    for (String id : new HashSet<>(mVungleBanners.keySet())) {
      VungleBannerAdapter banner = mVungleBanners.get(id);
      if (banner != null && !banner.isActive()) {
        banner = mVungleBanners.remove(id);
        if (banner != null) {
          banner.destroy();
        }
      }
    }
  }

  @Nullable
  synchronized VungleBannerAdapter getBannerRequest(
      @NonNull String placementId, @Nullable String requestUniqueId, @NonNull AdConfig adConfig) {
    cleanLeakedBannerAdapters();
    VungleBannerAdapter bannerRequest = mVungleBanners.get(placementId);
    if (bannerRequest != null) {
      String activeUniqueRequestId = bannerRequest.getUniquePubRequestId();
      Log.d(
          TAG, "activeUniqueId: " + activeUniqueRequestId + " ###  RequestId: " + requestUniqueId);

      if (activeUniqueRequestId == null) {
        Log.w(
            TAG,
            "Ad already loaded for placement ID: "
                + placementId
                + ", and cannot determine if "
                + "this is a refresh. Set Vungle extras when making an ad request to support "
                + "refresh on Vungle banner ads.");
        return null;
      }

      if (!activeUniqueRequestId.equals(requestUniqueId)) {
        Log.w(TAG, "Ad already loaded for placement ID: " + placementId);
        return null;
      }
    } else {
      bannerRequest = new VungleBannerAdapter(placementId, requestUniqueId, adConfig);
      mVungleBanners.put(placementId, bannerRequest);
    }

    Log.d(TAG, "New banner request:" + bannerRequest + "; size=" + mVungleBanners.size());
    return bannerRequest;
  }

  void removeActiveBannerAd(String placementId) {
    Log.d(TAG, "try to removeActiveBannerAd:" + placementId);
    VungleBannerAdapter activeBannerAd = mVungleBanners.remove(placementId);
    Log.d(TAG, "removeActiveBannerAd:" + activeBannerAd + "; size=" + mVungleBanners.size());
    if (activeBannerAd != null) {
      activeBannerAd.cleanUp();
    }
  }

  void storeActiveBannerAd(@NonNull String placementId, @NonNull VungleBannerAdapter instance) {
    if (!mVungleBanners.containsKey(placementId)) {
      mVungleBanners.put(placementId, instance);
      Log.d(TAG, "restoreActiveBannerAd:" + instance + "; size=" + mVungleBanners.size());
    }
  }
}
