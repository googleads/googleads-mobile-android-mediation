package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.MediationNativeListener;
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

class InMobiUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * InMobi native ad instance.
   */
  private final InMobiNative mInMobiNative;
  /**
   * Flag to check whether urls are returned for image assets.
   */
  private final boolean mIsOnlyURL;
  /**
   * MediationNativeListener instance.
   */
  private final MediationNativeListener mMediationNativeListener;
  /**
   * InMobi adapter instance.
   */
  private final InMobiAdapter mInMobiAdapter;

  public InMobiUnifiedNativeAdMapper(
      InMobiAdapter inMobiAdapter,
      InMobiNative inMobiNative,
      Boolean isOnlyURL,
      MediationNativeListener mediationNativeListener) {
    this.mInMobiAdapter = inMobiAdapter;
    this.mInMobiNative = inMobiNative;
    this.mIsOnlyURL = isOnlyURL;
    this.mMediationNativeListener = mediationNativeListener;
    setOverrideClickHandling(true);
    setOverrideImpressionRecording(true);
  }

  // Map InMobi Native Ad to AdMob Unified Native Ad.
  void mapUnifiedNativeAd(final Context context) {
    JSONObject payLoad;
    HashMap<String, URL> map;
    final Uri iconUri;
    final Double iconScale;

    try {
      if (mInMobiNative.getCustomAdContent() != null) {
        payLoad = mInMobiNative.getCustomAdContent();
      } else {
        mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
        return;
      }

      setHeadline(
          InMobiAdapterUtils.mandatoryChecking(
              mInMobiNative.getAdTitle(), InMobiNetworkValues.TITLE));
      setBody(
          InMobiAdapterUtils.mandatoryChecking(
              mInMobiNative.getAdDescription(), InMobiNetworkValues.DESCRIPTION));
      setCallToAction(
          InMobiAdapterUtils.mandatoryChecking(
              mInMobiNative.getAdCtaText(), InMobiNetworkValues.CTA));

      String landingURL =
          InMobiAdapterUtils.mandatoryChecking(
              mInMobiNative.getAdLandingPageUrl(), InMobiNetworkValues.LANDING_URL);
      Bundle paramMap = new Bundle();
      paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
      setExtras(paramMap);
      map = new HashMap<>();

      // App icon.
      URL iconURL = new URL(mInMobiNative.getAdIconUrl());
      iconUri = Uri.parse(iconURL.toURI().toString());
      iconScale = 1.0;
      if (!this.mIsOnlyURL) {
        map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
      } else {
        setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
        List<NativeAd.Image> imagesList = new ArrayList<>();
        imagesList.add(
            new InMobiNativeMappedImage(new ColorDrawable(Color.TRANSPARENT), null, 1.0));
        setImages(imagesList);
      }

    } catch (MandatoryParamException | MalformedURLException | URISyntaxException e) {
      e.printStackTrace();
      mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
      return;
    }

    try {
      if (payLoad.has(InMobiNetworkValues.RATING)) {
        setStarRating(Double.parseDouble(payLoad.getString(InMobiNetworkValues.RATING)));
      }
      if (payLoad.has(InMobiNetworkValues.PACKAGE_NAME)) {
        setStore("Google Play");
      } else {
        setStore("Others");
      }
      if (payLoad.has(InMobiNetworkValues.PRICE)) {
        setPrice(payLoad.getString(InMobiNetworkValues.PRICE));
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // Add primary view as media view
    final RelativeLayout placeHolderView = new RelativeLayout(context);
    placeHolderView.setLayoutParams(
        new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
    placeHolderView.setGravity(Gravity.CENTER);

    placeHolderView.post(
        new Runnable() {
          @Override
          public void run() {
            final View primaryView =
                mInMobiNative.getPrimaryViewOfWidth(
                    context, null, placeHolderView, placeHolderView.getWidth());
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
    boolean hasVideo = (mInMobiNative.isVideo() == null) ? false : mInMobiNative.isVideo();
    setHasVideoContent(hasVideo);

    // Download drawables.
    if (!this.mIsOnlyURL) {
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

              if ((null != iconDrawable)) {
                mMediationNativeListener.onAdLoaded(
                    mInMobiAdapter, InMobiUnifiedNativeAdMapper.this);
              } else {
                mMediationNativeListener.onAdFailedToLoad(
                    mInMobiAdapter, AdRequest.ERROR_CODE_NETWORK_ERROR);
              }
            }

            @Override
            public void onDownloadFailure() {
              mMediationNativeListener.onAdFailedToLoad(
                  mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
            }
          })
          .execute(map);
    } else {
      mMediationNativeListener.onAdLoaded(mInMobiAdapter, InMobiUnifiedNativeAdMapper.this);
    }
  }

  @Override
  public void recordImpression() {
    // All impression render events are fired automatically when the primary view is displayed
    // on the screen.
    // Reference: https://support.inmobi.com/monetize/android-guidelines/native-ads-for-android
  }

  @Override
  public void handleClick(View view) {
    // Handle click.
    mInMobiNative.reportAdClickAndOpenLandingPage();
  }

  @Override
  public void untrackView(View view) {
    mInMobiNative.pause();
  }

  @Override
  public void trackViews(
      View containerView,
      Map<String, View> clickableAssetViews,
      Map<String, View> nonclickableAssetViews) {
    mInMobiNative.resume();
  }
}
