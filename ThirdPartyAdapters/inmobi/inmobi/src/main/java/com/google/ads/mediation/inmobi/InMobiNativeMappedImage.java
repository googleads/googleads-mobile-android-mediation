// Copyright 2017 Google LLC
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

package com.google.ads.mediation.inmobi;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.formats.NativeAd;

/**
 * A {@link com.google.android.gms.ads.formats.NativeAd.Image} used to map InMobi native image asset
 * to Google native image asset.
 */
class InMobiNativeMappedImage extends NativeAd.Image {

  private final Drawable inMobiDrawable;
  private final Uri inMobiImageUri;
  private final double inMobiScale;

  public InMobiNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
    inMobiDrawable = drawable;
    inMobiImageUri = imageUri;
    inMobiScale = scale;
  }

  @NonNull
  @Override
  public Drawable getDrawable() {
    return inMobiDrawable;
  }

  @NonNull
  @Override
  public Uri getUri() {
    return inMobiImageUri;
  }

  @Override
  public double getScale() {
    return inMobiScale;
  }

}
