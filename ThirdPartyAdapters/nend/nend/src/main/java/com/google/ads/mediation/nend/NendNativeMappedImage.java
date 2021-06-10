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

  @Override
  public Drawable getDrawable() {
    return drawable;
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public double getScale() {
    return scale;
  }
}
