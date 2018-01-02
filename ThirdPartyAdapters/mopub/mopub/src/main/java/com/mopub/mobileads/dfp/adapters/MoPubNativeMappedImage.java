
package com.mopub.mobileads.dfp.adapters;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.gms.ads.formats.NativeAd;

/**
 * A {@link com.google.android.gms.ads.formats.NativeAd.Image} used to map MoPub native image asset
 * to a Google native image asset.
 */
public class MoPubNativeMappedImage extends NativeAd.Image {

    private Drawable mDrawable;
    private Uri mImageUri;
    private double mScale;

    public MoPubNativeMappedImage(Drawable drawable, String imageUrl, double scale) {
        mDrawable = drawable;
        mScale = scale;

        try {
            mImageUri = Uri.parse(imageUrl);
        } catch (Exception e) {
            Log.d(MoPubAdapter.TAG, "Exception trying to parse image URL.");
            return;
        }
    }
    
    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public Uri getUri() {
        return mImageUri;
    }

    @Override
    public double getScale() {
        return mScale;
    }

}
