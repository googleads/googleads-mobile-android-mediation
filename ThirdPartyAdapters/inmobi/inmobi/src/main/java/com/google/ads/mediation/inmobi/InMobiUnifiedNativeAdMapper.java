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
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.inmobi.ads.InMobiNative;
import com.inmobi.sdk.InMobiSdk;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

class InMobiUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * InMobi native ad instance.
   */
  private final InMobiNative inMobiNative;

  /**
   * Flag to check whether urls are returned for image assets.
   */
  private final boolean isOnlyURL;

  /**
   * MediationNativeListener instance.
   */
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback;

  private final InMobiNativeAd inMobiNativeAd;

  /**
   * InMobi adapter instance.
   */

  public InMobiUnifiedNativeAdMapper(InMobiNative inMobiNative,
                                     Boolean isOnlyURL, MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback, InMobiNativeAd inMobiNativeAd) {
    this.inMobiNative = inMobiNative;
    this.isOnlyURL = isOnlyURL;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
    this.inMobiNativeAd = inMobiNativeAd;
    setOverrideImpressionRecording(true);
  }

  // Map InMobi Native Ad to AdMob Unified Native Ad.
  void mapUnifiedNativeAd(final Context context) {

    if (!InMobiAdapterUtils.isValidNativeAd(inMobiNative)) {
      AdError error = InMobiConstants.createAdapterError(ERROR_MISSING_NATIVE_ASSETS,
              "InMobi native ad returned with a missing asset.");
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    setHeadline(inMobiNative.getAdTitle());
    setBody(inMobiNative.getAdDescription());
    setCallToAction(inMobiNative.getAdCtaText());

    // App icon.
    final URL iconURL;
    final Uri iconUri;
    final double iconScale = 1.0;

    try {
      iconURL = new URL(inMobiNative.getAdIconUrl());
      iconUri = Uri.parse(iconURL.toURI().toString());
    } catch (MalformedURLException | URISyntaxException exception) {
      AdError error = InMobiConstants.createAdapterError(ERROR_MALFORMED_IMAGE_URL, exception.getLocalizedMessage());
      Log.e(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    HashMap<String, URL> map = new HashMap<>();
    String landingURL = inMobiNative.getAdLandingPageUrl();
    Bundle paramMap = new Bundle();
    paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
    setExtras(paramMap);

    if (!this.isOnlyURL) {
      map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
    } else {
      setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
      List<NativeAd.Image> imagesList = new ArrayList<>();
      imagesList.add(
              new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
      setImages(imagesList);
    }

    // Optional assets.
    if (inMobiNative.getCustomAdContent() != null) {
      JSONObject payLoad = inMobiNative.getCustomAdContent();

      try {
        if (payLoad.has(InMobiNetworkValues.RATING)) {
          setStarRating(Double.parseDouble(payLoad.getString(InMobiNetworkValues.RATING)));
        }

        if (payLoad.has(InMobiNetworkValues.PRICE)) {
          setPrice(payLoad.getString(InMobiNetworkValues.PRICE));
        }
      } catch (JSONException jsonException) {
        Log.e(TAG, "InMobi custom native ad content payload could not be parsed. "
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
            new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
    placeHolderView.setGravity(Gravity.CENTER);
    placeHolderView.post(
            new Runnable() {
              @Override
              public void run() {
                final View primaryView =
                        inMobiNative.getPrimaryViewOfWidth(context, null, placeHolderView,
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
    boolean hasVideo = (inMobiNative.isVideo() == null) ? false : inMobiNative.isVideo();
    setHasVideoContent(hasVideo);

    // Download drawables.
    if (!this.isOnlyURL) {
      new ImageDownloaderAsyncTask(
              new ImageDownloaderAsyncTask.DrawableDownloadListener() {
                @Override
                public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {
                  Drawable iconDrawable = drawableMap.get(ImageDownloaderAsyncTask.KEY_ICON);
                  setIcon(new InMobiNativeMappedImage(iconDrawable, iconUri, iconScale));

                  List<NativeAd.Image> imagesList = new ArrayList<>();
                  imagesList.add(
                          new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
                  setImages(imagesList);

                  if (null != iconDrawable && mediationAdLoadCallback != null) {
                    inMobiNativeAd.mediationNativeAdCallback
                            = mediationAdLoadCallback.onSuccess(InMobiUnifiedNativeAdMapper.this);
                  } else {
                    AdError error = InMobiConstants.createAdapterError(ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,
                            "Failed to download image assets.");
                    Log.e(TAG, error.toString());
                    mediationAdLoadCallback.onFailure(error);
                  }
                }

                @Override
                public void onDownloadFailure() {
                  AdError error = InMobiConstants.createAdapterError(ERROR_NATIVE_ASSET_DOWNLOAD_FAILED,
                          "Failed to download image assets.");
                  Log.e(TAG, error.toString());
                  mediationAdLoadCallback.onFailure(error);
                }
              })
              .execute(map);
    } else {
      if (mediationAdLoadCallback != null) {
        inMobiNativeAd.mediationNativeAdCallback
                = mediationAdLoadCallback.onSuccess(InMobiUnifiedNativeAdMapper.this);
      }
    }
  }

  @Override
  public void handleClick(View view) {
    // Handle click.
    inMobiNative.reportAdClickAndOpenLandingPage();
  }

  @Override
  public void untrackView(View view) {
    inMobiNative.pause();
  }

  @Override
  public void trackViews(View containerView, Map<String, View> clickableAssetViews,
                         Map<String, View> nonclickableAssetViews) {
    inMobiNative.resume();
  }
}
