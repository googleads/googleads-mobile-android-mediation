package com.google.ads.mediation.nend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.formats.NativeAd;

public class NendNativeMappedImage extends NativeAd.Image {

    private Drawable drawable;
    private Uri uri;
    private double scale;

    NendNativeMappedImage(Context context, Bitmap bitmap, Uri uri) {
        this.uri = uri;
        if (bitmap != null) {
            drawable = new BitmapDrawable(context.getResources(), bitmap);
            scale = 1.0;
        }
    }

    @Override
    public @Nullable Drawable getDrawable() {
        return drawable;
    }

    @Override
    public @Nullable Uri getUri() {
        return uri;
    }

    @Override
    public double getScale() {
        return scale;
    }
}
