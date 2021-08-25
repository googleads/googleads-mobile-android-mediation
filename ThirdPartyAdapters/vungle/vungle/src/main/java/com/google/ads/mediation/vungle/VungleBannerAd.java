package com.google.ads.mediation.vungle;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.mediation.VungleBannerAdapter;
import com.vungle.warren.VungleBanner;
import java.lang.ref.WeakReference;

/**
 * This class is used to represent a Vungle Banner ad.
 */
public class VungleBannerAd {

  private static final String TAG = VungleBannerAd.class.getSimpleName();

  /**
   * Weak reference to the adapter owning this Vungle banner ad.
   */
  private final WeakReference<VungleBannerAdapter> adapter;

  /**
   * Vungle banner placement ID.
   */
  private final String placementId;

  /**
   * Vungle ad object for banner ads.
   */
  private VungleBanner vungleBanner;

  public VungleBannerAd(@NonNull String placementId, @NonNull VungleBannerAdapter adapter) {
    this.placementId = placementId;
    this.adapter = new WeakReference<>(adapter);
  }

  @Nullable
  public VungleBannerAdapter getAdapter() {
    return this.adapter.get();
  }

  public void setVungleBanner(@NonNull VungleBanner vungleBanner) {
    this.vungleBanner = vungleBanner;
  }

  @Nullable
  public VungleBanner getVungleBanner() {
    return vungleBanner;
  }

  public void attach() {
    VungleBannerAdapter bannerAdapter = adapter.get();
    if (bannerAdapter == null) {
      return;
    }

    RelativeLayout layout = bannerAdapter.getAdLayout();
    if (layout == null) {
      return;
    }

    if (vungleBanner != null && vungleBanner.getParent() == null) {
      layout.addView(vungleBanner);
    }
  }

  public void detach() {
    if (vungleBanner != null) {
      if (vungleBanner.getParent() != null) {
        ((ViewGroup) vungleBanner.getParent()).removeView(vungleBanner);
      }
    }
  }

  public void destroyAd() {
    if (vungleBanner != null) {
      Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + vungleBanner.hashCode());
      vungleBanner.destroyAd();
      vungleBanner = null;
    }
  }
}
