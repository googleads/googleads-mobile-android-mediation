package com.google.ads.mediation.imobile;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;

import java.util.ArrayList;
import java.util.List;

import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData;

/**
 * Mapper for UnifiedNativeAd.
 */
public final class IMobileUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

    /** Called when clicked. */
    private Runnable clickEvent;

    public IMobileUnifiedNativeAdMapper(ImobileSdkAdsNativeAdData adData, Drawable adImage) {
        // Initialize fields.
        this.clickEvent = adData.getClickEvent();

        // Set ad image.
        List<NativeAd.Image> images = new ArrayList<>(1);
        images.add(new NativeAdImage(adImage, null, 1));
        setImages(images);

        // Set ad data.
        setAdvertiser(adData.getSponsored());
        setBody(adData.getDescription());
        setCallToAction(Constants.CALL_TO_ACTION);
        setHeadline(adData.getTitle());

        //Created a transparent drawable as i-mobile do not render AdIcon.
        setIcon(new NativeAdImage(new ColorDrawable(Color.TRANSPARENT), null, 1));
    }

    @Override
    public void handleClick(View view) {
        // Run click event.
        clickEvent.run();
    }

    class NativeAdImage extends NativeAd.Image {

        private final Drawable drawable;
        private final Uri imageUri;
        private final double scale;

        NativeAdImage(Drawable drawable, Uri imageUri, double scale) {
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
}
