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

package com.google.ads.mediation.yahoo;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.formats.NativeAd;

class AdapterNativeMappedImage extends NativeAd.Image {

  /**
   * A drawable for the image.
   */
  private final Drawable drawable;

  /**
   * The image's URI.
   */
  private final Uri imageUri;

  /**
   * The image's scale.
   */
  private final double scale;

  public AdapterNativeMappedImage(final Drawable drawable, final Uri imageUri, final double scale) {
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
