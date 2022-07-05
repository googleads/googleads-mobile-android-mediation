package com.google.ads.mediation.facebook.rtb;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_CREATE_NATIVE_AD_FROM_BID_PAYLOAD;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_MAPPING_NATIVE_ASSETS;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_NULL_CONTEXT;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_WRONG_NATIVE_TYPE;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_ID;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.KEY_SOCIAL_CONTEXT_ASSET;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.TAG;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.getAdError;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.setMixedAudience;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.ads.Ad;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.ExtraHints;
import com.facebook.ads.MediaView;
import com.facebook.ads.MediaViewListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.google.ads.mediation.facebook.FacebookMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.UnifiedNativeAdAssetNames;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FacebookRtbNativeAd extends UnifiedNativeAdMapper {

  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback;
  private NativeAdBase mNativeAdBase;
  private MediationNativeAdCallback mNativeAdCallback;
  private MediaView mMediaView;

  public FacebookRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    this.callback = callback;
    this.adConfiguration = adConfiguration;
  }

  public void render() {
    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementID = FacebookMediationAdapter.getPlacementID(serverParameters);
    if (TextUtils.isEmpty(placementID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to request ad. PlacementID is null or empty.",
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      callback.onFailure(error);
      return;
    }

    setMixedAudience(adConfiguration);
    mMediaView = new MediaView(adConfiguration.getContext());

    try {
      mNativeAdBase = NativeAdBase.fromBidPayload(adConfiguration.getContext(), placementID,
          adConfiguration.getBidResponse());
    } catch (Exception ex) {
      AdError error = new AdError(ERROR_CREATE_NATIVE_AD_FROM_BID_PAYLOAD,
          "Failed to create native ad from bid payload: " + ex.getMessage(), ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      FacebookRtbNativeAd.this.callback.onFailure(error);
      return;
    }

    if (!TextUtils.isEmpty(adConfiguration.getWatermark())) {
      mNativeAdBase.setExtraHints(new ExtraHints.Builder()
          .mediationData(adConfiguration.getWatermark()).build());
    }

    mNativeAdBase.loadAd(
        mNativeAdBase.buildLoadAdConfig()
            .withAdListener(new NativeListener(adConfiguration.getContext(), mNativeAdBase))
            .withBid(adConfiguration.getBidResponse())
            .withMediaCacheFlag(NativeAdBase.MediaCacheFlag.ALL)
            .withPreloadedIconView(
                NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE,
                NativeAdBase.NativeAdLoadConfigBuilder.UNKNOWN_IMAGE_SIZE)
            .build());
  }

  private class NativeListener implements AdListener, NativeAdListener {

    /**
     * Context required to create AdOptions View.
     */
    private final WeakReference<Context> mContext;

    /**
     * Meta Audience Network native ad instance.
     */
    private final NativeAdBase mNativeAd;

    NativeListener(Context mContext, NativeAdBase mNativeAd) {
      this.mNativeAd = mNativeAd;
      this.mContext = new WeakReference<>(mContext);
    }

    @Override
    public void onAdClicked(Ad ad) {
      mNativeAdCallback.reportAdClicked();
      mNativeAdCallback.onAdOpened();
      mNativeAdCallback.onAdLeftApplication();
    }

    @Override
    public void onLoggingImpression(Ad ad) {
      // Google Mobile Ads handles impression tracking.
    }

    @Override
    public void onAdLoaded(Ad ad) {
      if (ad != mNativeAd) {
        AdError error = new AdError(ERROR_WRONG_NATIVE_TYPE, "Ad Loaded is not a Native Ad.",
            ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        FacebookRtbNativeAd.this.callback.onFailure(error);
        return;
      }

      Context context = mContext.get();
      if (context == null) {
        AdError error = new AdError(ERROR_NULL_CONTEXT, "Context is null.", ERROR_DOMAIN);
        Log.e(TAG, error.getMessage());
        FacebookRtbNativeAd.this.callback.onFailure(error);
        return;
      }

      FacebookRtbNativeAd.this.mapNativeAd(context, new NativeAdMapperListener() {
        @Override
        public void onMappingSuccess() {
          mNativeAdCallback = callback.onSuccess(FacebookRtbNativeAd.this);
        }

        @Override
        public void onMappingFailed(AdError error) {
          Log.w(TAG, error.getMessage());
          callback.onFailure(error);
        }
      });
    }

    @Override
    public void onError(Ad ad, com.facebook.ads.AdError adError) {
      AdError error = getAdError(adError);
      Log.w(TAG, error.getMessage());
      callback.onFailure(error);
    }

    @Override
    public void onMediaDownloaded(Ad ad) {
      Log.d(TAG, "onMediaDownloaded");
    }
  }

  /**
   * This method will map the Meta Audience Network {@link #mNativeAdBase} to this mapper and send
   * a success callback if the mapping was successful or a failure callback if the mapping was
   * unsuccessful.
   *
   * @param mapperListener used to send success/failure callbacks when mapping is done.
   */
  public void mapNativeAd(@NonNull Context context,
      @NonNull NativeAdMapperListener mapperListener) {
    if (!containsRequiredFieldsForUnifiedNativeAd(mNativeAdBase)) {
      AdError error = new AdError(ERROR_MAPPING_NATIVE_ASSETS,
          "Ad from Meta Audience Network doesn't have all required assets.", ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      mapperListener.onMappingFailed(error);
      return;
    }

    // Map all required assets (headline, one image, body, icon and call to
    // action).
    setHeadline(mNativeAdBase.getAdHeadline());
    if (mNativeAdBase.getAdCoverImage() != null) {
      List<com.google.android.gms.ads.formats.NativeAd.Image> images = new ArrayList<>();
      images.add(
          new FacebookAdapterNativeAdImage(Uri.parse(mNativeAdBase.getAdCoverImage().getUrl())));
      setImages(images);
    }
    setBody(mNativeAdBase.getAdBodyText());
    if (mNativeAdBase.getPreloadedIconViewDrawable() == null) {
      if (mNativeAdBase.getAdIcon() == null) {
        setIcon(new FacebookAdapterNativeAdImage());
      } else {
        setIcon(new FacebookAdapterNativeAdImage(Uri.parse(mNativeAdBase.getAdIcon().getUrl())));
      }
    } else {
      Drawable iconDrawable = mNativeAdBase.getPreloadedIconViewDrawable();
      FacebookAdapterNativeAdImage iconImage = new FacebookAdapterNativeAdImage(iconDrawable);
      setIcon(iconImage);
    }
    setCallToAction(mNativeAdBase.getAdCallToAction());
    setAdvertiser(mNativeAdBase.getAdvertiserName());

    mMediaView.setListener(new MediaViewListener() {
      @Override
      public void onPlay(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onVolumeChange(MediaView mediaView, float v) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onPause(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onComplete(MediaView mediaView) {
        if (mNativeAdCallback != null) {
          mNativeAdCallback.onVideoComplete();
        }
      }

      @Override
      public void onEnterFullscreen(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onExitFullscreen(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onFullscreenBackground(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }

      @Override
      public void onFullscreenForeground(MediaView mediaView) {
        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
      }
    });

    // Because the Meta Audience Network SDK doesn't offer a way to determine whether a native ad contains
    // a video asset or not, the adapter always returns a MediaView and claims to have
    // video content.
    FacebookRtbNativeAd.this.setHasVideoContent(true);
    FacebookRtbNativeAd.this.setMediaView(mMediaView);

    // Pass all the assets not supported by Google as extras.
    Bundle extras = new Bundle();
    extras.putCharSequence(KEY_ID, mNativeAdBase.getId());
    extras.putCharSequence(KEY_SOCIAL_CONTEXT_ASSET,
        FacebookRtbNativeAd.this.mNativeAdBase.getAdSocialContext());
    setExtras(extras);

    AdOptionsView adOptionsView = new AdOptionsView(context, mNativeAdBase, null);
    setAdChoicesContent(adOptionsView);
    mapperListener.onMappingSuccess();
  }

  /**
   * This method will check whether or not the given Meta Audience Network native ad contains all
   * the necessary fields for it to be mapped to Google Mobile Ads' native app install ad.
   *
   * @param nativeAd Meta Audience Network native ad.
   * @return {@code true} if the given ad contains all the necessary fields, {@link false}
   * otherwise.
   */
  private boolean containsRequiredFieldsForUnifiedNativeAd(NativeAdBase nativeAd) {
    boolean hasNativeBannerAdAssets = (nativeAd.getAdHeadline() != null)
        && (nativeAd.getAdBodyText() != null) && (nativeAd.getAdIcon() != null)
        && (nativeAd.getAdCallToAction() != null);
    if (nativeAd instanceof NativeBannerAd) {
      return hasNativeBannerAdAssets;
    }
    return hasNativeBannerAdAssets && (nativeAd.getAdCoverImage() != null) && (mMediaView != null);
  }

  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {

    // Meta Audience Network does its own click handling.
    setOverrideClickHandling(true);

    ArrayList<View> assetViews = new ArrayList<>(clickableAssetViews.values());
    View iconView = clickableAssetViews.get(UnifiedNativeAdAssetNames.ASSET_ICON);

    if (mNativeAdBase instanceof NativeBannerAd) {
      // trackViews() gets called after the ad loads, so forwarding onAdFailedToLoad() will be
      // too late.
      if (iconView == null) {
        Log.w(TAG, "Missing or invalid native ad icon asset. Meta Audience Network impression "
            + "recording might be impacted for this ad.");
        return;
      }

      if (!(iconView instanceof ImageView)) {
        String errorMessage = String.format("Native ad icon asset is rendered with an "
            + "incompatible class type. Meta Audience Network impression recording might be "
            + "impacted for this ad. Expected: ImageView, actual: %s.", iconView.getClass());
        Log.w(TAG, errorMessage);
        return;
      }

      NativeBannerAd nativeBannerAd = (NativeBannerAd) mNativeAdBase;
      nativeBannerAd.registerViewForInteraction(view, (ImageView) iconView, assetViews);
    } else if (mNativeAdBase instanceof NativeAd) {
      NativeAd nativeAd = (NativeAd) mNativeAdBase;
      if (iconView instanceof ImageView) {
        nativeAd.registerViewForInteraction(view, mMediaView, (ImageView) iconView, assetViews);
      } else {
        Log.w(TAG, "Native icon asset is not of type ImageView. "
            + "Calling registerViewForInteraction() without a reference to the icon view.");
        nativeAd.registerViewForInteraction(view, mMediaView, assetViews);
      }
    } else {
      Log.w(TAG, "Native ad type is not of type NativeAd or NativeBannerAd. "
          + "It is not currently supported by the Meta Audience Network Adapter. "
          + "Meta Audience Network impression recording might be impacted for this ad.");
    }
  }


  @Override
  public void untrackView(@NonNull View view) {
    if (mNativeAdBase != null) {
      mNativeAdBase.unregisterView();
    }
    super.untrackView(view);
  }

  private class FacebookAdapterNativeAdImage extends
      com.google.android.gms.ads.formats.NativeAd.Image {

    /**
     * A drawable for the Image.
     */
    private Drawable mDrawable;

    /**
     * An Uri from which the image can be obtained.
     */
    private Uri mUri;

    /**
     * Default constructor for {@link FacebookAdapterNativeAdImage}.
     */
    public FacebookAdapterNativeAdImage() {
    }

    /**
     * Default constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Uri}.
     *
     * @param uri required to initialize.
     */
    public FacebookAdapterNativeAdImage(Uri uri) {
      this.mUri = uri;
    }

    /**
     * Default constructor for {@link FacebookAdapterNativeAdImage}, requires an {@link Drawable}.
     *
     * @param drawable required to initialize.
     */
    public FacebookAdapterNativeAdImage(Drawable drawable) {
      this.mDrawable = drawable;
    }

    /**
     * Returns the native ad image drawable. This is purposefully set as {@link Nullable} even if
     * the overridden method is {@link NonNull}. The Google Mobile Ads SDK only supports loading
     * native ads of type {@link com.google.android.gms.ads.nativead.NativeAd}, which expose image
     * assets of type {@link com.google.android.gms.ads.nativead.NativeAd.Image}, which allows a
     * {@code null} drawable.
     *
     * @return the image drawable.
     */
    @Override
    @Nullable
    public Drawable getDrawable() {
      return mDrawable;
    }

    @Override
    @NonNull
    public Uri getUri() {
      return mUri;
    }

    @Override
    public double getScale() {
      // Default scale is 1.
      return 1;
    }
  }

  private interface NativeAdMapperListener {

    /**
     * This method will be called once the native ad mapping is successfully.
     */
    void onMappingSuccess();

    /**
     * This method will be called if the native ad mapping failed.
     */
    void onMappingFailed(AdError error);
  }

}
