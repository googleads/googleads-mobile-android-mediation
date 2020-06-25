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

/**
 * A {@link NativeAppInstallAdMapper} used to map an AppLovin Native ad to Google Native App Install
 * ad.
 */
class AppLovinNativeAdMapper extends NativeAppInstallAdMapper {

  /** AppLovin native ad instance. */
  private AppLovinNativeAd mNativeAd;

  AppLovinNativeAdMapper(AppLovinNativeAd nativeAd, Context context) {
    mNativeAd = nativeAd;
    setHeadline(nativeAd.getTitle());
    setBody(nativeAd.getDescriptionText());
    setCallToAction(nativeAd.getCtaText());

    ImageView mediaView = new ImageView(context);
    ViewGroup.LayoutParams layoutParams =
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    mediaView.setLayoutParams(layoutParams);

    ArrayList<NativeAd.Image> images = new ArrayList<>(1);
    Uri imageUri = Uri.parse(nativeAd.getImageUrl());
    Drawable imageDrawable = Drawable.createFromPath(imageUri.getPath());

    Uri iconUri = Uri.parse(nativeAd.getIconUrl());
    Drawable iconDrawable = Drawable.createFromPath(iconUri.getPath());

    AppLovinNativeAdImage image = new AppLovinNativeAdImage(imageUri, imageDrawable);
    AppLovinNativeAdImage icon = new AppLovinNativeAdImage(iconUri, iconDrawable);

    images.add(image);
    setImages(images);
    setIcon(icon);

    mediaView.setImageDrawable(imageDrawable);
    setMediaView(mediaView);
    setStarRating(nativeAd.getStarRating());

    Bundle extraAssets = new Bundle();
    extraAssets.putLong(AppLovinNativeAdapter.KEY_EXTRA_AD_ID, nativeAd.getAdId());
    extraAssets.putString(AppLovinNativeAdapter.KEY_EXTRA_CAPTION_TEXT, nativeAd.getCaptionText());
    setExtras(extraAssets);

    setOverrideClickHandling(false);
    setOverrideImpressionRecording(false);
  }

  @Override
  public void recordImpression() {
    mNativeAd.trackImpression();
  }

  @Override
  public void handleClick(View view) {
    mNativeAd.launchClickTarget(view.getContext());
  }
}
