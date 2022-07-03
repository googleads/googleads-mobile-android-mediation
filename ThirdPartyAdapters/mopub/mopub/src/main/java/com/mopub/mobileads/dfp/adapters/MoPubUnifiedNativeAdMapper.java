package com.mopub.mobileads.dfp.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.util.Drawables;
import com.mopub.nativeads.NativeImageHelper;
import com.mopub.nativeads.StaticNativeAd;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoPubUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * MoPub StaticNativeAd instance.
   */
  private StaticNativeAd mMoPubNativeAdData;
  /**
   * Holds privacy icon placement.
   */
  private int privacyIconPlacement;
  /**
   * Holds MoPub privacy information icon.
   */
  private ImageView privacyInformationIconImageView;
  /**
   * The size of MoPub's privacy icon.
   */
  private int mPrivacyIconSize;

  public MoPubUnifiedNativeAdMapper(
      @NonNull Context context,
      @NonNull StaticNativeAd ad,
      @Nullable Drawable icon,
      @Nullable Drawable nativeAdMainImage,
      int privacyIconPlacementParam,
      int privacyIconSize) {
    mMoPubNativeAdData = ad;
    setHeadline(mMoPubNativeAdData.getTitle());

    setBody(mMoPubNativeAdData.getText());

    setCallToAction(mMoPubNativeAdData.getCallToAction());
    privacyIconPlacement = privacyIconPlacementParam;
    mPrivacyIconSize = privacyIconSize;

    MoPubNativeMappedImage iconImage =
        new MoPubNativeMappedImage(
            icon, mMoPubNativeAdData.getIconImageUrl(), MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE);

    setIcon(iconImage);

    MoPubNativeMappedImage mainImage =
        new MoPubNativeMappedImage(
            nativeAdMainImage,
            mMoPubNativeAdData.getMainImageUrl(),
            MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE);

    List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
    imagesList.add(mainImage);
    setImages(imagesList);

    int height = nativeAdMainImage.getIntrinsicHeight();
    int width = nativeAdMainImage.getIntrinsicWidth();

    float aspectRatio = 0.0f;
    if (height > 0) {
      aspectRatio = (float) (width / height);
    }

    setMediaContentAspectRatio(aspectRatio);

    ImageView mediaView = new ImageView(context);
    mediaView.setImageDrawable(nativeAdMainImage);
    setMediaView(mediaView);

    setOverrideClickHandling(true);

    setOverrideImpressionRecording(true);
  }

  @Override
  public void trackViews(
      View view, Map<String, View> clickableAssets, Map<String, View> nonClickableAssets) {
    super.trackViews(view, clickableAssets, nonClickableAssets);
    mMoPubNativeAdData.prepare(view);

    if (!(view instanceof ViewGroup)) {
      return;
    }

    ViewGroup adView = (ViewGroup) view;

    View overlayView = adView.getChildAt(adView.getChildCount() - 1);
    if (overlayView instanceof FrameLayout) {

      final Context context = view.getContext();
      if (context == null) {
        return;
      }

      privacyInformationIconImageView = new ImageView(context);
      String privacyInformationImageUrl = mMoPubNativeAdData.getPrivacyInformationIconImageUrl();
      final String privacyInformationClickthroughUrl =
          mMoPubNativeAdData.getPrivacyInformationIconClickThroughUrl();

      if (privacyInformationImageUrl == null
          || TextUtils.isEmpty(privacyInformationClickthroughUrl)) {
        privacyInformationIconImageView.setImageDrawable(
            Drawables.NATIVE_PRIVACY_INFORMATION_ICON.createDrawable(context));
      } else {
        NativeImageHelper.loadImageView(
            privacyInformationImageUrl, privacyInformationIconImageView);
      }

      privacyInformationIconImageView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
              new UrlHandler.Builder()
                  .withSupportedUrlActions(
                      UrlAction.IGNORE_ABOUT_SCHEME,
                      UrlAction.OPEN_NATIVE_BROWSER,
                      UrlAction.OPEN_IN_APP_BROWSER,
                      UrlAction.HANDLE_SHARE_TWEET,
                      UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                      UrlAction.FOLLOW_DEEP_LINK)
                  .build()
                  .handleUrl(context, privacyInformationClickthroughUrl);
            }
          });

      privacyInformationIconImageView.setVisibility(View.VISIBLE);
      ((ViewGroup) overlayView).addView(privacyInformationIconImageView);

      float scale = context.getResources().getDisplayMetrics().density;
      int icon_size_px = (int) (mPrivacyIconSize * scale + 0.5);
      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(icon_size_px, icon_size_px);

      switch (privacyIconPlacement) {
        case NativeAdOptions.ADCHOICES_TOP_LEFT:
          params.gravity = Gravity.TOP | Gravity.START;
          break;
        case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
          params.gravity = Gravity.BOTTOM | Gravity.END;
          break;
        case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
          params.gravity = Gravity.BOTTOM | Gravity.START;
          break;
        case NativeAdOptions.ADCHOICES_TOP_RIGHT:
          params.gravity = Gravity.TOP | Gravity.END;
          break;
        default:
          params.gravity = Gravity.TOP | Gravity.END;
      }

      privacyInformationIconImageView.setLayoutParams(params);
      adView.requestLayout();
    } else {
      Log.d(MoPubAdapter.TAG, "Failed to show AdChoices icon.");
    }
  }

  @Override
  public void untrackView(View view) {
    super.untrackView(view);
    mMoPubNativeAdData.clear(view);

    if (privacyInformationIconImageView != null
        && privacyInformationIconImageView.getParent() != null) {
      ((ViewGroup) privacyInformationIconImageView.getParent())
          .removeView(privacyInformationIconImageView);
    }
  }

  @Override
  public void recordImpression() {
  }

  @Override
  public void handleClick(View view) {
  }
}