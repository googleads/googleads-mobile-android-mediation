package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_NULL_CONTEXT;
import static com.google.ads.mediation.nend.NendMediationAdapter.NEND_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAdOptions;
import net.nend.android.NendAdNative;
import net.nend.android.NendAdNative.Callback;
import net.nend.android.NendAdNativeClient;

class NativeAdLoader {

  /**
   * Listener class to forward Nend native ad events to the Google Mobile Ads SDK.
   */
  private final NendNativeAdForwarder forwarder;

  /**
   * Nend native ad client.
   */
  private final NendAdNativeClient client;

  /**
   * Custom options for requesting Nend native ads.
   */
  private final NativeAdOptions nativeAdOptions;

  /**
   * Indicates whether Nend has attempted to download their ad image bitmap. This is {@code true} if
   * Nend does not have an ad image to download.
   */
  private boolean isAdImageDownloadComplete;

  /**
   * Nend's native ad image.
   */
  private Bitmap nendAdImage = null;

  /**
   * Indicates whether Nend has attempted to download their logo image bitmap. This is {@code true}
   * if Nend does not have a logo image to download.
   */
  private boolean isLogoImageDownloadComplete;

  /**
   * Nend's native ad logo
   */
  private Bitmap nendLogoImage = null;

  NativeAdLoader(
      NendNativeAdForwarder forwarder, NendAdNativeClient client, NativeAdOptions nativeAdOptions) {
    this.forwarder = forwarder;
    this.client = client;
    this.nativeAdOptions = nativeAdOptions;
  }

  void loadAd() {
    client.loadAd(
        new NendAdNativeClient.Callback() {
          @Override
          public void onSuccess(final NendAdNative nendAdNative) {
            Context context = forwarder.getContextFromWeakReference();
            if (context == null) {
              AdError error = new AdError(ERROR_NULL_CONTEXT, "The context object is null.",
                  ERROR_DOMAIN);
              Log.e(TAG, error.getMessage());
              forwarder.failedToLoad(error);
              return;
            }

            downloadImages(
                nendAdNative,
                new OnNendImagesDownloadedListener() {
                  @Override
                  public void onImagesDownloaded(
                      @Nullable Bitmap adImage, @Nullable Bitmap logoImage) {
                    Context context = forwarder.getContextFromWeakReference();
                    if (context == null) {
                      AdError error = new AdError(ERROR_NULL_CONTEXT, "The context object is null.",
                          ERROR_DOMAIN);
                      Log.e(TAG, error.getMessage());
                      forwarder.failedToLoad(error);
                      return;
                    }

                    NendUnifiedNativeNormalAdMapper adMapper = createUnifiedNativeAdMapper(context,
                        nendAdNative);
                    forwarder.setUnifiedNativeAdMapper(adMapper);
                    forwarder.adLoaded();
                  }
                });
          }

          @Override
          public void onFailure(NendAdNativeClient.NendError nendError) {
            forwarder.setUnifiedNativeAdMapper(null);
            AdError error = new AdError(nendError.getCode(), nendError.getMessage(),
                NEND_SDK_ERROR_DOMAIN);
            Log.e(TAG, error.getMessage());
            forwarder.failedToLoad(error);
          }
        });
  }

  @NonNull
  private NendUnifiedNativeNormalAdMapper createUnifiedNativeAdMapper(@NonNull Context context,
      @NonNull NendAdNative ad) {
    NendNativeMappedImage adImage = null;
    if (nendAdImage != null) {
      String adImageUrl = ad.getAdImageUrl();
      if (!TextUtils.isEmpty(adImageUrl)) {
        adImage = new NendNativeMappedImage(context, nendAdImage, Uri.parse(adImageUrl));
      }
    }

    NendNativeMappedImage logoImage = null;
    if (nendLogoImage != null) {
      String logoImageUrl = ad.getLogoImageUrl();
      if (!TextUtils.isEmpty(logoImageUrl)) {
        logoImage = new NendNativeMappedImage(context, nendLogoImage, Uri.parse(logoImageUrl));
      }
    }

    return new NendUnifiedNativeNormalAdMapper(context, forwarder, ad, adImage, logoImage);
  }

  private void downloadImages(
      @NonNull final NendAdNative nendNativeAd,
      @NonNull final OnNendImagesDownloadedListener listener) {
    final Context context = forwarder.getContextFromWeakReference();
    if (context == null) {
      AdError error = new AdError(ERROR_NULL_CONTEXT, "The context object is null.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      forwarder.failedToLoad(error);
      return;
    }

    if (NendUnifiedNativeAdMapper.canDownloadImage(context, nendNativeAd.getAdImageUrl())) {
      nendNativeAd.downloadAdImage(
          new Callback() {
            @Override
            public void onSuccess(Bitmap adImageBitmap) {
              nendAdImage = adImageBitmap;
              isAdImageDownloadComplete = true;
              checkAndInvokeDownloadedListener(listener);
            }

            @Override
            public void onFailure(Exception exception) {
              Log.d(TAG, "Unable to download Nend ad image bitmap.", exception);
              isAdImageDownloadComplete = true;
              checkAndInvokeDownloadedListener(listener);
            }
          });
    } else {
      isAdImageDownloadComplete = true;
      checkAndInvokeDownloadedListener(listener);
    }

    if (NendUnifiedNativeAdMapper.canDownloadImage(context, nendNativeAd.getLogoImageUrl())) {
      nendNativeAd.downloadLogoImage(
          new Callback() {
            @Override
            public void onSuccess(Bitmap adLogoBitmap) {
              nendLogoImage = adLogoBitmap;
              isLogoImageDownloadComplete = true;
              checkAndInvokeDownloadedListener(listener);
            }

            @Override
            public void onFailure(Exception exception) {
              Log.d(TAG, "Unable to download Nend logo image bitmap.", exception);
              isLogoImageDownloadComplete = true;
              checkAndInvokeDownloadedListener(listener);
            }
          });
    } else {
      isLogoImageDownloadComplete = true;
      checkAndInvokeDownloadedListener(listener);
    }
  }

  private void checkAndInvokeDownloadedListener(@NonNull OnNendImagesDownloadedListener listener) {
    if (isAdImageDownloadComplete && isLogoImageDownloadComplete) {
      listener.onImagesDownloaded(nendAdImage, nendLogoImage);
    }
  }

  interface OnNendImagesDownloadedListener {

    /**
     * Invoked when nend has finished attempting to download all image assets.
     *
     * @param adImage   nend's ad image bitmap asset
     * @param logoImage nend's logo image bitmap asset
     */
    void onImagesDownloaded(@Nullable Bitmap adImage, @Nullable Bitmap logoImage);
  }
}
