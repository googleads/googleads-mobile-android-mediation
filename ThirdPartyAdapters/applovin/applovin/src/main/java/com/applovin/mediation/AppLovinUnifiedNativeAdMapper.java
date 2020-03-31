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
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.util.ArrayList;

/**
 * A {@link UnifiedNativeAdMapper} used to map an AppLovin Native ad to Google Unified Native Ad.
 */
class AppLovinUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * AppLovin native ad instance.
   */
  private AppLovinNativeAd mNativeAd;

  public AppLovinUnifiedNativeAdMapper(Context context, AppLovinNativeAd nativeAd) {
    mNativeAd = nativeAd;
    setHeadline(mNativeAd.getTitle());
    setBody(mNativeAd.getDescriptionText());
    setCallToAction(mNativeAd.getCtaText());

    final ImageView mediaView = new ImageView(context);
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    mediaView.setLayoutParams(layoutParams);

    ArrayList<NativeAd.Image> images = new ArrayList<>(1);
    Uri imageUri = Uri.parse(mNativeAd.getImageUrl());
    Drawable imageDrawable = Drawable.createFromPath(imageUri.getPath());

    Uri iconUri = Uri.parse(mNativeAd.getIconUrl());
    Drawable iconDrawable = Drawable.createFromPath(iconUri.getPath());

    AppLovinNativeAdImage image = new AppLovinNativeAdImage(imageUri, imageDrawable);
    AppLovinNativeAdImage icon = new AppLovinNativeAdImage(iconUri, iconDrawable);

    images.add(image);
    setImages(images);
    setIcon(icon);

    mediaView.setImageDrawable(imageDrawable);
    setMediaView(mediaView);
    int imageHeight = imageDrawable.getIntrinsicHeight();
    if (imageHeight > 0) {
      setMediaContentAspectRatio(imageDrawable.getIntrinsicWidth() / imageHeight);
    }
    setStarRating((double) mNativeAd.getStarRating());

    Bundle extraAssets = new Bundle();
    extraAssets.putLong(AppLovinNativeAdapter.KEY_EXTRA_AD_ID, mNativeAd.getAdId());
    extraAssets.putString(AppLovinNativeAdapter.KEY_EXTRA_CAPTION_TEXT, mNativeAd.getCaptionText());
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
