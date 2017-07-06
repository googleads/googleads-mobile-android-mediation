/**
 * Class responsible to fit AdMob image to the InMobi image standards
 */
package com.google.ads.mediation.inmobi;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.gms.ads.formats.NativeAd;

/**
 * @author yogeesh.rajendra@com.google.ads.mediation.inmobi.com
 *
 */
class InMobiNativeMappedImage extends NativeAd.Image {

    private final Drawable inMobiDrawable;
    private final Uri inmobiImageUri;
    private final double inMobiScale;

    public InMobiNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
        inMobiDrawable = drawable;
        inmobiImageUri = imageUri;
        inMobiScale = scale;
    }

    @Override
    public Drawable getDrawable() {
        return inMobiDrawable;
    }

    @Override
    public Uri getUri() {
        return inmobiImageUri;
    }

    @Override
    public double getScale() {
        return inMobiScale;
    }

}
