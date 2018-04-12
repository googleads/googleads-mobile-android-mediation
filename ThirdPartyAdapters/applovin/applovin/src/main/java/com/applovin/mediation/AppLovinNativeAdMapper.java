package com.applovin.mediation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.applovin.nativeAds.AppLovinNativeAd;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;

import java.util.ArrayList;

public class AppLovinNativeAdMapper extends NativeAppInstallAdMapper {

    private AppLovinNativeAd nativeAd;

    public AppLovinNativeAdMapper(AppLovinNativeAd nativeAd, Context context) {
        this.nativeAd = nativeAd;
        setHeadline(nativeAd.getTitle());
        setBody(nativeAd.getDescriptionText());
        setCallToAction(nativeAd.getCtaText());
        ImageView mediaView = new ImageView(context);
        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mediaView.setLayoutParams(layoutParams);

        ArrayList<NativeAd.Image> images = new ArrayList<>(1);
        AppLovinNativeAdImage image;
        AppLovinNativeAdImage icon;
        Drawable imageDrawable =
                Drawable.createFromPath(Uri.parse(nativeAd.getImageUrl()).getPath());
        Drawable iconDrawable = Drawable.createFromPath(Uri.parse(nativeAd.getIconUrl()).getPath());
        image = new AppLovinNativeAdImage(Uri.parse(nativeAd.getImageUrl()), imageDrawable);
        icon = new AppLovinNativeAdImage(Uri.parse(nativeAd.getIconUrl()), iconDrawable);
        images.add(image);
        setImages(images);
        setIcon(icon);

        mediaView.setImageDrawable(imageDrawable);
        setMediaView(mediaView);

        Bundle extraAssets = new Bundle();
        extraAssets.putLong(AppLovinNativeAdapter.KEY_EXTRA_AD_ID, nativeAd.getAdId());
        extraAssets.putString(
                AppLovinNativeAdapter.KEY_EXTRA_CAPTION_TEXT, nativeAd.getCaptionText());

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);
    }

    @Override
    public void recordImpression() {
        super.recordImpression();
        nativeAd.trackImpression();
    }

    @Override
    public void handleClick(View view) {
        super.handleClick(view);
        nativeAd.launchClickTarget(view.getContext());
    }

    /**
     * A {@link NativeAd.Image} class used to map AppLovin native image to AdMob native image.
     */
    private class AppLovinNativeAdImage extends NativeAd.Image {

        private Drawable drawable;
        private Uri uri;

        public AppLovinNativeAdImage(Uri uri, Drawable drawable) {
            this.drawable = drawable;
            this.uri = uri;
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
            // AppLovin SDK does not support scale, return 1 by default.
            return 1;
        }
    }
}
