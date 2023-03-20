// Copyright 2019 Google LLC
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

package com.google.ads.mediation.imobile;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.util.ArrayList;
import java.util.List;
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData;

/**
 * Mapper for UnifiedNativeAd.
 */
public final class IMobileUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * Called when clicked.
   */
  private final Runnable clickEvent;

  public IMobileUnifiedNativeAdMapper(
      @NonNull ImobileSdkAdsNativeAdData adData, @NonNull Drawable adImage) {
    // Initialize fields.
    this.clickEvent = adData.getClickEvent();

    // Set ad image.
    List<NativeAd.Image> images = new ArrayList<>(1);
    images.add(new NativeAdImage(adImage, null, 1));
    setImages(images);
    int height = adImage.getIntrinsicHeight();
    if (height > 0) {
      setMediaContentAspectRatio(adImage.getIntrinsicWidth() / height);
    }

    // Set ad data.
    setAdvertiser(adData.getSponsored());
    setBody(adData.getDescription());
    setCallToAction(Constants.CALL_TO_ACTION);
    setHeadline(adData.getTitle());

    // Created a transparent drawable as i-mobile do not render AdIcon.
    setIcon(new NativeAdImage(new ColorDrawable(Color.TRANSPARENT), null, 1));
  }

  @Override
  public void handleClick(@NonNull View view) {
    // Run click event.
    clickEvent.run();
  }

  class NativeAdImage extends NativeAd.Image {

    private final Drawable drawable;
    private final Uri imageUri;
    private final double scale;

    NativeAdImage(Drawable drawable, Uri imageUri, double scale) {
      this.drawable = drawable;
      this.imageUri = imageUri;
      this.scale = scale;
    }

    @NonNull
    @Override
    public Drawable getDrawable() {
      return drawable;
    }

    @NonNull
    @Override
    public Uri getUri() {
      return imageUri;
    }

    @Override
    public double getScale() {
      return scale;
    }
  }
}
