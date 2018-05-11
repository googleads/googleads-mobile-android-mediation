package com.google.ads.mediation.dap.forwarder;


import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.gms.ads.formats.NativeAd;

public class DuNativeMappedImage extends NativeAd.Image {

    private Drawable mDrawable;
    private Uri mImageUri;

    DuNativeMappedImage(Uri imageUri) {
        mImageUri = imageUri;
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    void setDrawable(Drawable mDrawable) {
        this.mDrawable = mDrawable;
    }

    @Override
    public Uri getUri() {
        return mImageUri;
    }

    @Override
    public double getScale() {
        return 1;
    }
}
