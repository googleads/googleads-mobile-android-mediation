package com.google.ads.mediation.mytarget;

import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_INVALID_NATIVE_AD_LOADED;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MISSING_REQUIRED_NATIVE_ASSET;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MY_TARGET_SDK;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_NON_UNIFIED_NATIVE_REQUEST;
import static com.google.ads.mediation.mytarget.MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.mytarget.MyTargetTools.handleMediationExtras;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.my.target.common.CachePolicy;
import com.my.target.common.CustomParams;
import com.my.target.common.models.ImageData;
import com.my.target.nativeads.MediationHelper;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.my.target.nativeads.views.MediaAdView;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * A {@link MediationNativeAdapter} to load and show myTarget native ads.
 */
public class MyTargetNativeAdapter implements MediationNativeAdapter {

  private static final String TAG = "MyTargetNativeAdapter";

  public static final String EXTRA_KEY_AGE_RESTRICTIONS = "ageRestrictions";
  public static final String EXTRA_KEY_ADVERTISING_LABEL = "advertisingLabel";
  public static final String EXTRA_KEY_CATEGORY = "category";
  public static final String EXTRA_KEY_SUBCATEGORY = "subcategory";
  public static final String EXTRA_KEY_VOTES = "votes";

  @Nullable
  private MediationNativeListener nativeListener;

  private static int findMediaAdViewPosition(@NonNull List<View> clickableViews,
      @NonNull MediaAdView mediaAdView) {
    for (int i = 0; i < clickableViews.size(); i++) {
      View view = clickableViews.get(i);
      if (view instanceof com.google.android.gms.ads.nativead.MediaView || view instanceof com.google.android.gms.ads.formats.MediaView) {
        FrameLayout mediaView = (FrameLayout) view;
        int childCount = mediaView.getChildCount();
        for (int j = 0; j < childCount; j++) {
          View innerView = mediaView.getChildAt(j);
          if (innerView == mediaAdView) {
            return i;
          }
        }
        break;
      }
    }
    return -1;
  }

  @Override
  public void requestNativeAd(Context context, MediationNativeListener mediationNativeListener,
      Bundle serverParameter, NativeMediationAdRequest nativeMediationAdRequest,
      Bundle customEventExtras) {
    this.nativeListener = mediationNativeListener;

    if (!nativeMediationAdRequest.isUnifiedNativeAdRequested()) {
      AdError error = new AdError(ERROR_NON_UNIFIED_NATIVE_REQUEST,
          "Unified Native Ads should be requested.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationNativeListener.onAdFailedToLoad(MyTargetNativeAdapter.this, error);
      return;
    }

    int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameter);
    if (slotId < 0) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      nativeListener.onAdFailedToLoad(MyTargetNativeAdapter.this, error);
      return;
    }

    NativeAdOptions options = null;
    int gender = 0;
    Date birthday = null;
    if (nativeMediationAdRequest != null) {
      options = nativeMediationAdRequest.getNativeAdOptions();
      gender = nativeMediationAdRequest.getGender();
      birthday = nativeMediationAdRequest.getBirthday();
    }

    NativeAd nativeAd = new NativeAd(slotId, context);

    int cachePolicy = CachePolicy.IMAGE;
    if (options != null) {
      if (options.shouldReturnUrlsForImageAssets()) {
        cachePolicy = CachePolicy.NONE;
      }
      Log.d(TAG, "Set cache policy to " + cachePolicy);
    }
    nativeAd.setCachePolicy(cachePolicy);

    CustomParams params = nativeAd.getCustomParams();
    handleMediationExtras(TAG, customEventExtras, params);
    Log.d(TAG, "Set gender to " + gender);
    params.setGender(gender);

    if (birthday != null && birthday.getTime() != -1) {
      GregorianCalendar calendar = new GregorianCalendar();
      GregorianCalendar calendarNow = new GregorianCalendar();

      calendar.setTimeInMillis(birthday.getTime());
      int age = calendarNow.get(GregorianCalendar.YEAR) - calendar.get(GregorianCalendar.YEAR);
      if (age >= 0) {
        params.setAge(age);
      }
    }

    MyTargetNativeAdListener nativeAdListener = new MyTargetNativeAdListener(nativeAd, context);

    params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
    nativeAd.setListener(nativeAdListener);
    nativeAd.load();
  }

  @Override
  public void onDestroy() {
    nativeListener = null;
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  /**
   * A {@link Image} used to map a myTarget native ad image to a Google native ad image.
   */
  private static class MyTargetAdmobNativeImage extends Image {

    @NonNull
    private final Uri uri;

    @Nullable
    private Drawable drawable;

    MyTargetAdmobNativeImage(@NonNull ImageData imageData, @NonNull Resources resources) {
      Bitmap bitmap = imageData.getBitmap();
      if (bitmap != null) {
        drawable = new BitmapDrawable(resources, bitmap);
      }
      uri = Uri.parse(imageData.getUrl());
    }

    @Nullable
    @Override
    public Drawable getDrawable() {
      return drawable;
    }

    @NonNull
    @Override
    public Uri getUri() {
      return uri;
    }

    @Override
    public double getScale() {
      return 1;
    }
  }

  /**
   * A {@link MyTargetNativeUnifiedAdMapper} used to map myTarget native ad to Google Mobile Ads SDK
   * native app unified ad.
   */
  private static class MyTargetNativeUnifiedAdMapper extends UnifiedNativeAdMapper {

    @NonNull
    private final NativeAd nativeAd;

    @NonNull
    private final MediaAdView mediaAdView;

    MyTargetNativeUnifiedAdMapper(@NonNull NativeAd nativeAd, @NonNull Context context) {
      this.nativeAd = nativeAd;
      this.mediaAdView = new MediaAdView(context);
      setOverrideClickHandling(true);
      setOverrideImpressionRecording(true);
      NativePromoBanner banner = nativeAd.getBanner();
      if (banner == null) {
        return;
      }
      setBody(banner.getDescription());
      setCallToAction(banner.getCtaText());
      setHeadline(banner.getTitle());
      ImageData icon = banner.getIcon();
      if (icon != null && !TextUtils.isEmpty(icon.getUrl())) {
        setIcon(new MyTargetAdmobNativeImage(icon, context.getResources()));
      }
      ImageData image = banner.getImage();
      setHasVideoContent(true);
      if (mediaAdView.getMediaAspectRatio() > 0) {
        setMediaContentAspectRatio(mediaAdView.getMediaAspectRatio());
      }
      setMediaView(mediaAdView);
      if (image != null && !TextUtils.isEmpty(image.getUrl())) {
        ArrayList<Image> imageArrayList = new ArrayList<>();
        imageArrayList.add(new MyTargetAdmobNativeImage(image, context.getResources()));
        setImages(imageArrayList);
      }
      setAdvertiser(banner.getDomain());
      setStarRating((double) banner.getRating());
      setStore(null);
      setPrice(null);

      Bundle extras = new Bundle();
      final String ageRestrictions = banner.getAgeRestrictions();
      if (!TextUtils.isEmpty(ageRestrictions)) {
        extras.putString(EXTRA_KEY_AGE_RESTRICTIONS, ageRestrictions);
      }
      final String advertisingLabel = banner.getAdvertisingLabel();
      if (!TextUtils.isEmpty(advertisingLabel)) {
        extras.putString(EXTRA_KEY_ADVERTISING_LABEL, advertisingLabel);
      }
      final String category = banner.getCategory();
      if (!TextUtils.isEmpty(category)) {
        extras.putString(EXTRA_KEY_CATEGORY, category);
      }
      final String subCategory = banner.getSubCategory();
      if (!TextUtils.isEmpty(subCategory)) {
        extras.putString(EXTRA_KEY_SUBCATEGORY, subCategory);
      }
      final int votes = banner.getVotes();
      if (votes > 0) {
        extras.putInt(EXTRA_KEY_VOTES, votes);
      }
      setExtras(extras);
    }

    @Override
    public void trackViews(final View containerView, final Map<String, View> clickables,
        Map<String, View> nonclickables) {
      final ArrayList<View> clickableViews = new ArrayList<>(clickables.values());
      containerView.post(new Runnable() {
        @Override
        public void run() {
          int mediaPosition = findMediaAdViewPosition(clickableViews, mediaAdView);
          if (mediaPosition >= 0) {
            clickableViews.remove(mediaPosition);
            clickableViews.add(mediaAdView);
          }
          MediationHelper.registerView(nativeAd, containerView, clickableViews, mediaAdView);
        }
      });
    }

    @Override
    public void untrackView(View view) {
      nativeAd.unregisterView();
    }
  }

  /**
   * A {@link MyTargetNativeAdListener} used to forward myTarget native ads events to Google.
   */
  private class MyTargetNativeAdListener implements NativeAd.NativeAdListener {

    @NonNull
    private final NativeAd nativeAd;

    @NonNull
    private final Context context;

    MyTargetNativeAdListener(final @NonNull NativeAd nativeAd, final @NonNull Context context) {
      this.nativeAd = nativeAd;
      this.context = context;
    }

    @Override
    public void onLoad(@NonNull NativePromoBanner banner, @NonNull NativeAd nativeAd) {
      if (this.nativeAd != nativeAd) {
        AdError error = new AdError(ERROR_INVALID_NATIVE_AD_LOADED,
            "Loaded native ad object does not match the requested ad object.", ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        if (nativeListener != null) {
          nativeListener.onAdFailedToLoad(MyTargetNativeAdapter.this, error);
        }
        return;
      }

      mapAd(nativeAd, banner);
    }

    @Override
    public void onNoAd(@NonNull final String reason, @NonNull final NativeAd nativeAd) {
      AdError error = new AdError(ERROR_MY_TARGET_SDK, reason, MY_TARGET_SDK_ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      if (nativeListener != null) {
        nativeListener.onAdFailedToLoad(MyTargetNativeAdapter.this, error);
      }
    }

    @Override
    public void onClick(@NonNull final NativeAd nativeAd) {
      Log.d(TAG, "Ad clicked.");
      if (nativeListener != null) {
        nativeListener.onAdClicked(MyTargetNativeAdapter.this);
        nativeListener.onAdOpened(MyTargetNativeAdapter.this);
        nativeListener.onAdLeftApplication(MyTargetNativeAdapter.this);
      }
    }

    @Override
    public void onShow(@NonNull final NativeAd nativeAd) {
      Log.d(TAG, "Ad show.");
      if (nativeListener != null) {
        nativeListener.onAdImpression(MyTargetNativeAdapter.this);
      }
    }

    @Override
    public void onVideoPlay(@NonNull NativeAd nativeAd) {
      Log.d(TAG, "Play ad video.");
    }

    @Override
    public void onVideoPause(@NonNull NativeAd nativeAd) {
      Log.d(TAG, "Pause ad video.");
    }

    @Override
    public void onVideoComplete(@NonNull NativeAd nativeAd) {
      Log.d(TAG, "Complete ad video.");
      if (nativeListener != null) {
        nativeListener.onVideoEnd(MyTargetNativeAdapter.this);
      }
    }

    private void mapAd(final @NonNull NativeAd nativeAd, final @NonNull NativePromoBanner banner) {
      if (banner.getImage() == null || banner.getIcon() == null) {
        AdError error = new AdError(ERROR_MISSING_REQUIRED_NATIVE_ASSET,
            "Native ad is missing one of the following required assets: image or icon.",
            ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        if (nativeListener != null) {
          nativeListener.onAdFailedToLoad(MyTargetNativeAdapter.this, error);
        }
        return;
      }

      MyTargetNativeUnifiedAdMapper unifiedMapper = new MyTargetNativeUnifiedAdMapper(nativeAd,
          context);
      Log.d(TAG, "Ad loaded successfully.");
      if (nativeListener != null) {
        nativeListener.onAdLoaded(MyTargetNativeAdapter.this, unifiedMapper);
      }
    }
  }
}
