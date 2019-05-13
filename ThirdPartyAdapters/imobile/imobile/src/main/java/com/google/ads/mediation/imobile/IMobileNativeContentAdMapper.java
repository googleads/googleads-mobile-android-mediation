package com.google.ads.mediation.imobile;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.NativeContentAdMapper;

import java.util.ArrayList;
import java.util.List;

import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData;

/**
 * Mapper for NativeContentAd.
 */
public final class IMobileNativeContentAdMapper extends NativeContentAdMapper {

    /** Called when clicked. */
    private Runnable clickEvent;

    /** Ad image. */
    private Drawable adDrawable;

    public IMobileNativeContentAdMapper(ImobileSdkAdsNativeAdData adData, Bitmap adImage) {
        // Initialize fields.
        this.clickEvent = adData.getClickEvent();
        this.adDrawable = new BitmapDrawable(adImage);

        // Set ad image.
        List<NativeAd.Image> images = new ArrayList<>(1);
        images.add(new NativeAd.Image() {
            @Override
            public Drawable getDrawable() {
                return adDrawable;
            }

            @Override
            public Uri getUri() {
                return null;
            }

            @Override
            public double getScale() {
                return 1;
            }
        });
        setImages(images);

        // Set ad data.
        setAdvertiser(adData.getSponsored());
        setBody(adData.getDescription());
        setCallToAction(Constants.CALL_TO_ACTION);
        setHeadline(adData.getTitle());
    }

    @Override
    public void handleClick(View view) {
        // Run click event.
        clickEvent.run();
    }
}
