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
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.google.ads.mediation.inmobi.renderers.InMobiNativeAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.inmobi.ads.InMobiNative;
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
  private final InMobiNative imNative;

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

  public InMobiUnifiedNativeAdMapper(@NonNull InMobiNative inMobiNative, Boolean isOnlyURL,
      MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
          mediationAdLoadCallback, InMobiNativeAd inMobiNativeAd) {
    this.imNative = inMobiNative;
    this.isOnlyURL = isOnlyURL;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiNativeAd = inMobiNativeAd;
    setOverrideImpressionRecording(true);
  }

  // Map InMobi Native Ad to AdMob Unified Native Ad.
  public void mapUnifiedNativeAd(final Context context) {
    if (!InMobiAdapterUtils.isValidNativeAd(imNative)) {
      AdError error = InMobiConstants.createAdapterError(ERROR_MISSING_NATIVE_ASSETS,
          "InMobi native ad returned with a missing asset.");
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    setHeadline(imNative.getAdTitle());
    setBody(imNative.getAdDescription());
    setCallToAction(imNative.getAdCtaText());

    // App icon.
    final URL iconURL;
    final Uri iconUri;
    final double iconScale = 1.0;

    try {
      iconURL = new URL(imNative.getAdIconUrl());
      iconUri = Uri.parse(iconURL.toURI().toString());
    } catch (MalformedURLException | URISyntaxException exception) {
      AdError error = InMobiConstants.createAdapterError(ERROR_MALFORMED_IMAGE_URL,
          exception.getLocalizedMessage());
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    HashMap<String, URL> map = new HashMap<>();
    String landingURL = imNative.getAdLandingPageUrl();
    Bundle paramMap = new Bundle();
    paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
    setExtras(paramMap);

    if (!this.isOnlyURL) {
      map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
    } else {
      setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
      List<NativeAd.Image> imagesList = new ArrayList<>();
      imagesList.add(new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
      setImages(imagesList);
    }

    // Optional assets.
    if (imNative.getCustomAdContent() != null) {
      JSONObject payLoad = imNative.getCustomAdContent();

      try {
        if (payLoad.has(InMobiNetworkValues.RATING)) {
          setStarRating(Double.parseDouble(payLoad.getString(InMobiNetworkValues.RATING)));
        }

        if (payLoad.has(InMobiNetworkValues.PRICE)) {
          setPrice(payLoad.getString(InMobiNetworkValues.PRICE));
        }
      } catch (JSONException jsonException) {
        Log.w(TAG, "InMobi custom native ad content payload could not be parsed. "
            + "The returned native ad will not have star rating or price values.");
      }

      if (payLoad.has(InMobiNetworkValues.PACKAGE_NAME)) {
        setStore("Google Play");
      } else {
        setStore("Others");
      }
    }

    // Add primary view as media view
    final RelativeLayout placeHolderView = new ClickInterceptorRelativeLayout(context);
    placeHolderView.setLayoutParams(
        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT));
    placeHolderView.setGravity(Gravity.CENTER);
    placeHolderView.post(new Runnable() {
      @Override
      public void run() {
        final View primaryView = imNative.getPrimaryViewOfWidth(context, null, placeHolderView,
            placeHolderView.getWidth());
        if (primaryView == null) {
          return;
        }

        placeHolderView.addView(primaryView);
        int viewHeight = primaryView.getLayoutParams().height;
        if (viewHeight > 0) {
          setMediaContentAspectRatio((float) primaryView.getLayoutParams().width / viewHeight);
        }
      }
    });

    setMediaView(placeHolderView);
    boolean hasVideo = (imNative.isVideo() == null) ? false : imNative.isVideo();
    setHasVideoContent(hasVideo);

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

  @Override
  public void handleClick(View view) {
    // Handle click.
    imNative.reportAdClickAndOpenLandingPage();
  }

  @Override
  public void untrackView(View view) {
    imNative.pause();
  }

  @Override
  public void trackViews(View containerView, Map<String, View> clickableAssetViews,
      Map<String, View> nonclickableAssetViews) {
    imNative.resume();
  }
}
