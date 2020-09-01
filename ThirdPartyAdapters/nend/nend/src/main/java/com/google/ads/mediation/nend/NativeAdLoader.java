package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import net.nend.android.NendAdNative;
import net.nend.android.NendAdNative.Callback;
import net.nend.android.NendAdNativeClient;

class NativeAdLoader {

  /** Listener class to forward Nend native ad events to the Google Mobile Ads SDK. */
  private NendNativeAdForwarder forwarder;

  /** Nend native ad client. */
  private NendAdNativeClient client;

  /** Custom options for requesting Nend native ads. */
  private NativeAdOptions nativeAdOptions;

  /**
   * Indicates whether Nend has attempted to download their ad image bitmap. This is {@code true} if
   * Nend does not have an ad image to download.
   */
  private boolean isAdImageDownloadComplete;

  /** Nend's native ad image. */
  private Bitmap nendAdImage = null;

  /**
   * Indicates whether Nend has attempted to download their logo image bitmap. This is {@code true}
   * if Nend does not have a logo image to download.
   */
  private boolean isLogoImageDownloadComplete;

  /** Nend's native ad logo */
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
              Log.e(TAG, "Your context may be released...");
              forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
              return;
            }

            downloadImages(
                nendAdNative,
                new OnNendImagesDownloadedListener() {
                  @Override
                  public void onImagesDownloaded(
                      @Nullable Bitmap adImage, @Nullable Bitmap logoImage) {
                    NendUnifiedNativeNormalAdMapper adMapper =
                        createUnifiedNativeAdMapper(nendAdNative);
                    if (adMapper == null) {
                      Log.e(TAG, "Failed to create unified native ad mapper.");
                      forwarder.failedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                      return;
                    }

                    forwarder.setUnifiedNativeAdMapper(adMapper);
                    forwarder.adLoaded();
                  }
                });
          }

          @Override
          public void onFailure(NendAdNativeClient.NendError nendError) {
            Log.e(TAG, "Failed to request Nend native ad: " + nendError.getMessage());
            forwarder.setUnifiedNativeAdMapper(null);
            forwarder.failedToLoad(nendError.getCode());
          }
        });
  }

  @Nullable
  private NendUnifiedNativeNormalAdMapper createUnifiedNativeAdMapper(@NonNull NendAdNative ad) {
    Context context = forwarder.getContextFromWeakReference();
    if (context == null) {
      Log.e(TAG, "Your context may be released...");
      forwarder.failedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
      return null;
    }

    boolean shouldReturnUrlsForImageAssets = false;
    if (nativeAdOptions != null) {
      shouldReturnUrlsForImageAssets = nativeAdOptions.shouldReturnUrlsForImageAssets();
    }

    NendNativeMappedImage adImage = null;
    if (shouldReturnUrlsForImageAssets || nendAdImage != null) {
      adImage = new NendNativeMappedImage(context, nendAdImage, Uri.parse(ad.getAdImageUrl()));
    }

    NendNativeMappedImage logoImage = null;
    if (shouldReturnUrlsForImageAssets || nendLogoImage != null) {
      logoImage = new NendNativeMappedImage(context, nendAdImage, Uri.parse(ad.getLogoImageUrl()));
    }

    return new NendUnifiedNativeNormalAdMapper(context, forwarder, ad, adImage, logoImage);
  }

  private void downloadImages(
      @NonNull final NendAdNative nendNativeAd,
      @NonNull final OnNendImagesDownloadedListener listener) {
    final Context context = forwarder.getContextFromWeakReference();
    if (context == null) {
      Log.e(TAG, "Your context may be released...");
      forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
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
     * @param adImage nend's ad image bitmap asset
     * @param logoImage nend's logo image bitmap asset
     */
    void onImagesDownloaded(@Nullable Bitmap adImage, @Nullable Bitmap logoImage);
  }
}
