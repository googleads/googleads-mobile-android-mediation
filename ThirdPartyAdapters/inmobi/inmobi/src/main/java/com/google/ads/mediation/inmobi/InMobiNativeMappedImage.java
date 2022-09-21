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
