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

package com.google.ads.mediation.nend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.formats.NativeAd;

public class NendNativeMappedImage extends NativeAd.Image {

  private Drawable drawable;
  private final Uri uri;
  private double scale;

  NendNativeMappedImage(@NonNull Context context, @Nullable Bitmap bitmap, @NonNull Uri uri) {
    this.uri = uri;
    if (bitmap != null) {
      drawable = new BitmapDrawable(context.getResources(), bitmap);
      scale = 1.0;
    }
  }

  @NonNull
  @Override
  public Drawable getDrawable() {
    return drawable;
  }

  @NonNull
  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public double getScale() {
    return scale;
  }
}
