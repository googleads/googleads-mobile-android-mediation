// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_MALFORMED_IMAGE_URL;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_MISSING_NATIVE_ASSETS;
import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_NATIVE_ASSET_DOWNLOAD_FAILED;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.inmobi.media.ads.nativeAd.InMobiNativeViewData;
import com.inmobi.media.ads.nativeAd.MediaView;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class InMobiUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * InMobi native ad instance.
   */
  private final InMobiNativeWrapper inMobiNativeWrapper;

  /**
   * Flag to check whether urls are returned for image assets.
   */

  private final boolean isOnlyURL;

  /**
   * Callback that fires on loading success or failure.
   */
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      mediationAdLoadCallback;

  private final InMobiNativeAd inMobiNativeAd;

  public InMobiUnifiedNativeAdMapper(@NonNull InMobiNativeWrapper inMobiNativeWrapper, Boolean isOnlyURL,
      MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          mediationAdLoadCallback, InMobiNativeAd inMobiNativeAd) {
    this.inMobiNativeWrapper = inMobiNativeWrapper;
    this.isOnlyURL = isOnlyURL;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiNativeAd = inMobiNativeAd;
    setOverrideImpressionRecording(true);
  }

  // Map InMobi Native Ad to AdMob Unified Native Ad.
  public void mapUnifiedNativeAd(final Context context) {
    if (inMobiNativeWrapper.getAdTitle() != null) {
      setHeadline(inMobiNativeWrapper.getAdTitle());
    }

    if (inMobiNativeWrapper.getAdDescription() != null) {
      setBody(inMobiNativeWrapper.getAdDescription());
    }

    if (inMobiNativeWrapper.getAdCtaText() != null) {
      setCallToAction(inMobiNativeWrapper.getAdCtaText());
    }

    if (inMobiNativeWrapper.getAdvertiserName() != null) {
      setAdvertiser(inMobiNativeWrapper.getAdvertiserName());
    }

    setStarRating((double) inMobiNativeWrapper.getAdRating());

    // Add primary view as media view
    final MediaView mediaView = inMobiNativeWrapper.getMediaView();
    if (mediaView != null) setMediaView(mediaView);
    setHasVideoContent(inMobiNativeWrapper.isVideo());

    if (inMobiNativeWrapper.getAdIconUrl() != null) {
      // App icon.
      final URL iconURL;
      final Uri iconUri;
      final double iconScale = 1.0;

      try {
        iconURL = new URL(inMobiNativeWrapper.getAdIconUrl());
        iconUri = Uri.parse(iconURL.toURI().toString());
      } catch (MalformedURLException | URISyntaxException exception) {
        AdError error = InMobiConstants.createAdapterError(ERROR_MALFORMED_IMAGE_URL,
                exception.getLocalizedMessage());
        Log.w(TAG, error.toString());
        mediationAdLoadCallback.onFailure(error);
        return;
      }

      HashMap<String, URL> map = new HashMap<>();

      if (!this.isOnlyURL) {
        map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
      } else {
        setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
        List<NativeAd.Image> imagesList = new ArrayList<>();
        imagesList.add(new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
        setImages(imagesList);
      }

      // Download drawables.
      if (!this.isOnlyURL) {
        new ImageDownloaderAsyncTask(new ImageDownloaderAsyncTask.DrawableDownloadListener() {
          @Override
          public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {
            Drawable iconDrawable = drawableMap.get(ImageDownloaderAsyncTask.KEY_ICON);
            setIcon(new InMobiNativeMappedImage(iconDrawable, iconUri, iconScale));

            List<NativeAd.Image> imagesList = new ArrayList<>();
            imagesList.add(
                    new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
            setImages(imagesList);

            if (null != iconDrawable && mediationAdLoadCallback != null) {
              inMobiNativeAd.mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(
                      InMobiUnifiedNativeAdMapper.this);
            } else {
              AdError error = InMobiConstants.createAdapterError(ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,
                      "InMobi SDK failed to download native ad image assets.");
              Log.w(TAG, error.toString());
              mediationAdLoadCallback.onFailure(error);
            }
          }

          @Override
          public void onDownloadFailure() {
            AdError error = InMobiConstants.createAdapterError(ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,
                    "InMobi SDK failed to download native ad image assets.");
            Log.w(TAG, error.toString());
            mediationAdLoadCallback.onFailure(error);
          }
        }).execute(map);
      } else {
        if (mediationAdLoadCallback != null) {
          inMobiNativeAd.mediationNativeAdCallback = mediationAdLoadCallback.onSuccess(
                  InMobiUnifiedNativeAdMapper.this);
        }
      }

    }
  }


  @Override
  public void untrackView(View view) {
    inMobiNativeWrapper.unTrackViews();
  }

  @Override
  public void trackViews(View containerView, Map<String, View> clickableAssetViews,
      Map<String, View> nonclickableAssetViews) {
    setOverrideClickHandling(true);

    View titleView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_HEADLINE);
    View descriptionView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_BODY);
    View iconView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_ICON);
    View ctaView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_CALL_TO_ACTION);
    View advertiserView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_ADVERTISER);
    View ratingView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_STAR_RATING);

    InMobiNativeViewData.Builder viewDataBuilder = new InMobiNativeViewData.Builder((ViewGroup) containerView);

    if (titleView != null) {
      viewDataBuilder.setTitleView(titleView);
    }

    if (descriptionView != null) {
      viewDataBuilder.setDescriptionView(descriptionView);
    }

    if (iconView instanceof ImageView) {
      viewDataBuilder.setIconView((ImageView) iconView);
    }

    if (ctaView != null) {
      viewDataBuilder.setCTAView(ctaView);
    }

    if (advertiserView != null) {
      viewDataBuilder.setAdvertiserView(advertiserView);
    }

    if (ratingView != null) {
      viewDataBuilder.setRatingView(ratingView);
    }

    inMobiNativeWrapper.registerForTracking(viewDataBuilder.build());
  }
}
