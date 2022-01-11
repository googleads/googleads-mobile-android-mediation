package com.vungle.mediation;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Vungle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to load and show Vungle ads and keep track of {@link VungleBannerAd} instances.
 */
public class VungleManager {

  private static final String TAG = VungleManager.class.getSimpleName();
  private static final String PLAYING_PLACEMENT = "placementID";

  private static VungleManager sInstance;

  private final ConcurrentHashMap<String, VungleBannerAd> mVungleBanners;
  private final ConcurrentHashMap<String, VungleNativeAd> mVungleNativeAds;

  public static synchronized VungleManager getInstance() {
    if (sInstance == null) {
      sInstance = new VungleManager();
    }
    return sInstance;
  }

  private VungleManager() {
    mVungleBanners = new ConcurrentHashMap<>();
    mVungleNativeAds = new ConcurrentHashMap<>();
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
  public boolean isValidPlacement(String placementId) {
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

  public synchronized boolean canRequestBannerAd(@NonNull String placementId,
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

  public void registerBannerAd(@NonNull String placementId, @NonNull VungleBannerAd instance) {
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

  public void removeActiveNativeAd(@NonNull String placementId,
      @Nullable VungleNativeAd activeNativeAd) {
    Log.d(TAG, "try to removeActiveNativeAd: " + placementId);

    boolean didRemove = mVungleNativeAds.remove(placementId, activeNativeAd);
    if (didRemove && activeNativeAd != null) {
      Log.d(TAG, "removeActiveNativeAd: " + activeNativeAd + "; size=" + mVungleNativeAds.size());
      activeNativeAd.destroyAd();
    }
  }

  public void registerNativeAd(@NonNull String placementId, @NonNull VungleNativeAd instance) {
    removeActiveNativeAd(placementId, mVungleNativeAds.get(placementId));
    if (!mVungleNativeAds.containsKey(placementId)) {
      mVungleNativeAds.put(placementId, instance);
      Log.d(TAG, "registerNativeAd: " + instance + "; size=" + mVungleNativeAds.size());
    }
  }

  public boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(
        TAG,
        "Found closest ad size: " + closestSize.toString() + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(BANNER_SHORT);
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      adConfig.setAdSize(BANNER);
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(VUNGLE_MREC);
    }

    return true;
  }
}
