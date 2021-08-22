/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.yandex.mobile.ads.nativeads;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.yandex.R;
import com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdAsset;
import com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdImage;
import com.google.ads.mediation.yandex.nativeads.asset.YandexNativeAdImageFactory;
import com.google.android.gms.ads.formats.NativeAd.Image;

import java.util.Collections;

public class YandexNativeAdMappersFactory {

    private static final boolean OVERRIDE_CLICK_TRACKING = true;
    private static final boolean OVERRIDE_IMPRESSION_RECORDING = true;

    @NonNull
    private final YandexNativeAdImageFactory mNativeAdImageFactory;

    public YandexNativeAdMappersFactory() {
        mNativeAdImageFactory = new YandexNativeAdImageFactory();
    }

    @NonNull
    public YandexNativeAdMapper createAdMapper(@NonNull final Context context,
                                               @NonNull final NativeAd nativeAd,
                                               @Nullable final Bundle extras) {
        final Bundle networkExtras = extras == null ? new Bundle() : extras;
        final NativeAdAssets assets = nativeAd.getAdAssets();
        final TextView feedbackView = createFeedbackView(context, assets);
        final YandexNativeAdMapper mapper =
                new YandexNativeAdMapper(nativeAd, networkExtras, feedbackView);
        mapper.setOverrideClickHandling(OVERRIDE_CLICK_TRACKING);
        mapper.setOverrideImpressionRecording(OVERRIDE_IMPRESSION_RECORDING);

        mapper.setAdvertiser(assets.getSponsored());
        mapper.setBody(assets.getBody());
        mapper.setCallToAction(assets.getCallToAction());
        mapper.setHeadline(assets.getTitle());
        mapper.setPrice(assets.getPrice());
        mapper.setStore(assets.getDomain());
        if (assets.getRating() != null) {
            final Double starRating = Double.valueOf(assets.getRating());
            mapper.setStarRating(starRating);
        }
        setIcon(context, assets, mapper);
        setImages(context, mapper, assets);
        setMedia(mapper, assets);

        setAdChoices(mapper, feedbackView);

        final MediaView mediaView = new MediaView(context);
        mapper.setMediaView(mediaView);

        final Bundle assetExtras = createAssetExtras(assets);
        mapper.setExtras(assetExtras);

        return mapper;
    }

    private void setIcon(@NonNull final Context context,
                         @NonNull final NativeAdAssets assets,
                         @NonNull final YandexNativeAdMapper mapper) {
        mapper.setIcon(mNativeAdImageFactory.createYandexNativeAdImage(context, assets.getIcon()));
    }

    private void setImages(@NonNull final Context context,
                           @NonNull final YandexNativeAdMapper mapper,
                           @NonNull final NativeAdAssets nativeAdAssets) {
        final NativeAdImage nativeAdImage = nativeAdAssets.getImage();
        if (nativeAdImage != null) {
            final int width = nativeAdImage.getWidth();
            final int height = nativeAdImage.getHeight();
            final YandexNativeAdImage yandexNativeAdImage =
                    mNativeAdImageFactory.createYandexNativeAdImage(context, nativeAdImage);
            mapper.setImages(Collections.<Image>singletonList(yandexNativeAdImage));

            if (height != 0) {
                final float aspectRatio = width / (float) height;
                mapper.setHasVideoContent(true);
                mapper.setMediaContentAspectRatio(aspectRatio);
            }
        }
    }

    private void setMedia(@NonNull final YandexNativeAdMapper mapper,
                          @NonNull final NativeAdAssets nativeAdAssets) {
        final NativeAdMedia media = nativeAdAssets.getMedia();
        if (media != null) {
            final float aspectRatio = media.getAspectRatio();
            mapper.setHasVideoContent(true);
            mapper.setMediaContentAspectRatio(aspectRatio);
        }
    }

    @NonNull
    private Bundle createAssetExtras(@NonNull final NativeAdAssets assets) {
        final Bundle assetExtras = new Bundle();
        assetExtras.putString(YandexNativeAdAsset.AGE, assets.getAge());
        assetExtras.putString(YandexNativeAdAsset.REVIEW_COUNT, assets.getReviewCount());
        assetExtras.putString(YandexNativeAdAsset.WARNING, assets.getWarning());

        return assetExtras;
    }

    @Nullable
    private TextView createFeedbackView(@NonNull final Context context,
                                        @NonNull final NativeAdAssets nativeAdAssets) {
        final TextView feedbackView;
        if (nativeAdAssets.isFeedbackAvailable()) {
            final Resources resources = context.getResources();
            final Drawable feedbackDrawable = resources
                    .getDrawable(R.drawable.admob_yandex_ad_choices);

            feedbackView = new TextView(context);
            feedbackView.setBackground(feedbackDrawable);
        } else {
            feedbackView = null;
        }
        return feedbackView;
    }

    private void setAdChoices(@NonNull final YandexNativeAdMapper mapper,
                              @Nullable final TextView feedbackView) {
        if (feedbackView != null) {
            mapper.setAdChoicesContent(feedbackView);
        }
    }
}
