package com.google.ads.mediation.verizon;

import android.graphics.drawable.Drawable;
import android.net.Uri;
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

  public AdapterNativeMappedImage(final Drawable drawable, final Uri imageUri,
      final double scale) {

    this.drawable = drawable;
    this.imageUri = imageUri;
    this.scale = scale;
  }

  @Override
  public Drawable getDrawable() {

    return drawable;
  }

  @Override
  public Uri getUri() {

    return imageUri;
  }

  @Override
  public double getScale() {

    return scale;
  }
}
