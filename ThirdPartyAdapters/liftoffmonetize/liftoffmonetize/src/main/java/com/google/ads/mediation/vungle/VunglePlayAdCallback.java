// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.vungle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.mediation.VungleBannerAdapter;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;

/**
 * Liftoff Monetize adapter implementation of {@link PlayAdCallback}. Since the Vungle SDK keeps a
 * strong mapping of ads with strong references to callbacks, this callback class must have no
 * strong references to an adapter object.
 */
public class VunglePlayAdCallback implements PlayAdCallback {

  private final WeakReference<VungleBannerAdapter> adapterReference;
  private final WeakReference<PlayAdCallback> callbackReference;
  private final VungleBannerAd vungleBannerAd;

  public VunglePlayAdCallback(@NonNull PlayAdCallback callback,
      @NonNull VungleBannerAdapter adapter, @Nullable VungleBannerAd vungleBannerAd) {
    this.callbackReference = new WeakReference<>(callback);
    this.adapterReference = new WeakReference<>(adapter);
    this.vungleBannerAd = vungleBannerAd;
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  @Override
  public void onAdStart(String placementID) {
    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onAdStart(placementID);
    }
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
    // Deprecated, No-op.
  }

  @Override
  public void onAdEnd(String placementID) {
    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onAdEnd(placementID);
    }
  }

  @Override
  public void onAdClick(String placementID) {
    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onAdClick(placementID);
    }
  }

  @Override
  public void onAdRewarded(String placementID) {
    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onAdRewarded(placementID);
    }
  }

  @Override
  public void onAdLeftApplication(String placementID) {
    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onAdLeftApplication(placementID);
    }
  }

  @Override
  public void onError(String placementID, VungleException exception) {
    VungleManager.getInstance().removeActiveBannerAd(placementID, vungleBannerAd);

    PlayAdCallback callback = callbackReference.get();
    VungleBannerAdapter adapter = adapterReference.get();
    if (callback != null && adapter != null && adapter.isRequestPending()) {
      callback.onError(placementID, exception);
    }
  }

  @Override
  public void onAdViewed(String placementID) {
    // No-op. To be mapped to respective adapter events in future release.
  }
}