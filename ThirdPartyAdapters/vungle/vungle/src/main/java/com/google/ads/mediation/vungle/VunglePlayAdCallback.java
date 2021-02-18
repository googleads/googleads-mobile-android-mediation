package com.google.ads.mediation.vungle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.mediation.VungleBannerAdapter;
import com.vungle.mediation.VungleListener;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;

/**
 * Vungle adapter implementation of {@link PlayAdCallback}. Since the Vungle SDK keeps a strong
 * mapping of ads with strong references to callbacks, this callback class must have no strong
 * references to an adapter object.
 */
public class VunglePlayAdCallback implements PlayAdCallback {

  private final WeakReference<VungleBannerAdapter> adapterReference;
  private final WeakReference<VungleListener> listenerReference;
  private final VungleBannerAd vungleBannerAd;

  public VunglePlayAdCallback(@NonNull VungleListener listener,
      @NonNull VungleBannerAdapter adapter, @Nullable VungleBannerAd vungleBannerAd) {
    this.listenerReference = new WeakReference<>(listener);
    this.adapterReference = new WeakReference<>(adapter);
    this.vungleBannerAd = vungleBannerAd;
  }

  @Override
  public void onAdStart(String placementID) {
    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdStart(placementID);
    }
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
  }

  @Override
  public void onAdEnd(String placementID) {
    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdEnd(placementID);
    }
  }

  @Override
  public void onAdClick(String placementID) {
    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdClick(placementID);
    }
  }

  @Override
  public void onAdRewarded(String placementID) {
    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdRewarded(placementID);
    }
  }

  @Override
  public void onAdLeftApplication(String placementID) {
    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdLeftApplication(placementID);
    }
  }

  @Override
  public void onError(String placementID, VungleException exception) {
    VungleManager.getInstance().removeActiveBannerAd(placementID, vungleBannerAd);

    VungleListener listener = listenerReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (listener != null && adapter != null && adapter.isRequestPending()) {
      listener.onAdFailedToLoad(exception.getExceptionCode());
    }
  }

  @Override
  public void onAdViewed(String placementID) {
    // No-op. To be mapped to respective adapter events in future release.
  }
}
