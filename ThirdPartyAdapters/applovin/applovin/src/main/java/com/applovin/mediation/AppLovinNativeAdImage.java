package com.applovin.mediation;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.gms.ads.formats.NativeAd;

public class AppLovinNativeAdImage extends NativeAd.Image {

    private final Drawable mDrawable;
    private final Uri mUri;

    AppLovinNativeAdImage(Uri uri, Drawable drawable) {
        mDrawable = drawable;
        mUri = uri;
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public double getScale() {
        // AppLovin SDK does not provide scale, return 1 by default.
        return 1;
    }
}
