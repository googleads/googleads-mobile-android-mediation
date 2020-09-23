package com.google.ads.mediation.inmobi;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.google.android.gms.ads.formats.NativeAd;

/**
 * A {@link com.google.android.gms.ads.formats.NativeAd.Image} used to map InMobi native image asset
 * to Google native image asset.
 */
class InMobiNativeMappedImage extends NativeAd.Image {

  private final Drawable mInMobiDrawable;
  private final Uri mInmobiImageUri;
  private final double mInMobiScale;

  public InMobiNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
    mInMobiDrawable = drawable;
    mInmobiImageUri = imageUri;
    mInMobiScale = scale;
  }

  @Override
  public Drawable getDrawable() {
    return mInMobiDrawable;
  }

  @Override
  public Uri getUri() {
    return mInmobiImageUri;
  }

  @Override
  public double getScale() {
    return mInMobiScale;
  }

}
