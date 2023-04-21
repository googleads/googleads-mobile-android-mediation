// Copyright 2022 Google LLC
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

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vungle.warren.AdConfig;
import com.vungle.warren.NativeAd;
import com.vungle.warren.NativeAdLayout;
import com.vungle.warren.NativeAdListener;
import com.vungle.warren.ui.view.MediaView;

/**
 * This class is used to represent a Liftoff Monetize Native ad.
 */
public class VungleNativeAd {

  private final String placementId;

  private final NativeAdLayout nativeAdLayout;

  private final MediaView mediaView;

  /**
   * Liftoff Monetize ad object for native ads.
   */
  private final NativeAd nativeAd;

  public VungleNativeAd(@NonNull Context context, @NonNull String placementId,
      boolean isLifeCycleManagementDisabled) {
    this.placementId = placementId;
    this.nativeAd = new NativeAd(context, placementId);
    this.nativeAdLayout = new NativeAdLayout(context);
    this.nativeAdLayout.disableLifeCycleManagement(isLifeCycleManagementDisabled);
    this.mediaView = new MediaView(context);
  }

  public void loadNativeAd(@Nullable AdConfig adConfig, @Nullable String adMarkup,
      @Nullable NativeAdListener listener) {
    nativeAd.loadAd(adConfig, adMarkup, listener);
  }

  @Nullable
  public NativeAd getNativeAd() {
    return nativeAd;
  }

  public NativeAdLayout getNativeAdLayout() {
    return nativeAdLayout;
  }

  public MediaView getMediaView() {
    return mediaView;
  }

  public void destroyAd() {
    if (nativeAdLayout != null) {
      nativeAdLayout.removeAllViews();
      if (nativeAdLayout.getParent() != null) {
        ((ViewGroup) nativeAdLayout.getParent()).removeView(nativeAdLayout);
      }
    }

    if (mediaView != null) {
      mediaView.removeAllViews();
      if (mediaView.getParent() != null) {
        ((ViewGroup) mediaView.getParent()).removeView(mediaView);
      }
    }

    if (nativeAd != null) {
      Log.d(TAG, "Liftoff Monetize native adapter cleanUp: destroyAd # "
          + nativeAd.hashCode());
      nativeAd.unregisterView();
      nativeAd.destroy();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return " [placementId="
        + placementId
        + " # nativeAdLayout="
        + nativeAdLayout
        + " # mediaView="
        + mediaView
        + " # nativeAd="
        + nativeAd
        + " # hashcode="
        + hashCode()
        + "] ";
  }
}
