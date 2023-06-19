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

import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.mediation.VungleBannerAdapter;
import com.vungle.warren.VungleBanner;
import java.lang.ref.WeakReference;

/**
 * This class is used to represent a Liftoff Monetize Banner ad.
 */
public class VungleBannerAd {

  /**
   * Weak reference to the adapter owning this Liftoff Monetize banner ad.
   */
  private final WeakReference<VungleBannerAdapter> adapter;

  /**
   * Liftoff Monetize banner placement ID.
   */
  private final String placementId;

  /**
   * Liftoff Monetize ad object for banner ads.
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
      Log.d(TAG, "Liftoff Monetize banner adapter cleanUp: destroyAd # "
          + vungleBanner.hashCode());
      vungleBanner.destroyAd();
      vungleBanner = null;
    }
  }
}