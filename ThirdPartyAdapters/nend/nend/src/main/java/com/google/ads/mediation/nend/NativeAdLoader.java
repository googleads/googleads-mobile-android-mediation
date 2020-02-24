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
import net.nend.android.NendAdNativeClient;

class NativeAdLoader {

    /**
     * Listener class to forward Nend native ad events to the Google Mobile Ads SDK.
     */
    private NendNativeAdForwarder forwarder;

    /**
     * Nend native ad client.
     */
    private NendAdNativeClient client;

    /**
     * Custom options for requesting Nend native ads.
     */
    private NativeAdOptions nativeAdOptions;

    private NendAdNativeClient.Callback normalAdLoaderCallback = new NendAdNativeClient.Callback() {
        @Override
        public void onSuccess(NendAdNative nendAdNative) {
            Context context = forwarder.getContextFromWeakReference();
            if (context == null) {
                Log.e(TAG, "Your context may be released...");
                forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }

            if (NendUnifiedNativeAdMapper.canDownloadImage(context, nendAdNative.getAdImageUrl())) {
                if (nativeAdOptions != null && nativeAdOptions.shouldReturnUrlsForImageAssets()) {
                    mappingUnifiedNativeNormalAd(nendAdNative, true, null, null);
                } else {
                    downloadImageThenMappingUnifiedNativeNormalAd(nendAdNative);
                }
            } else {
                mappingUnifiedNativeNormalAd(nendAdNative, false, null, null);
            }
        }

        @Override
        public void onFailure(NendAdNativeClient.NendError nendError) {
            forwarder.setUnifiedNativeAdMapper(null);
            switch (nendError) {
                case INVALID_INTERVAL_MILLIS:
                case FAILED_AD_REQUEST:
                    forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
                case INVALID_RESPONSE_TYPE:
                    forwarder.failedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
                default:
                    break;
            }
        }
    };

    private void mappingUnifiedNativeNormalAd(@NonNull NendAdNative ad,
            @Nullable Bitmap adImageBitmap) {
        mappingUnifiedNativeNormalAd(ad, false, adImageBitmap, null);
    }

    private void mappingUnifiedNativeNormalAd(@NonNull NendAdNative ad,
            boolean shouldReturnUrlsForImageAssets, @Nullable Bitmap adImageBitmap,
            @Nullable Bitmap logoImageBitmap) {
        Context context = forwarder.getContextFromWeakReference();
        if (context == null) {
            Log.e(TAG, "Your context may be released...");
            forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        NendNativeMappedImage adImage = null;
        if (shouldReturnUrlsForImageAssets || adImageBitmap != null) {
            adImage =  new NendNativeMappedImage(context, adImageBitmap, Uri.parse(ad.getAdImageUrl()));
        }

        NendNativeMappedImage logoImage = null;
        if (shouldReturnUrlsForImageAssets || logoImageBitmap != null) {
            logoImage = new NendNativeMappedImage(context, logoImageBitmap, Uri.parse(ad.getLogoImageUrl()));
        }

        NendUnifiedNativeNormalAdMapper normalAdMapper =
            new NendUnifiedNativeNormalAdMapper(context, forwarder, ad, adImage, logoImage);
        forwarder.setUnifiedNativeAdMapper(normalAdMapper);
        forwarder.adLoaded();
    }

    private void downloadImageThenMappingUnifiedNativeNormalAd(final NendAdNative nendAdNative) {
        nendAdNative.downloadAdImage(new NendAdNative.Callback() {
            @Override
            public void onSuccess(final Bitmap adImageBitmap) {
                Context context = forwarder.getContextFromWeakReference();
                if (context == null) {
                    Log.e(TAG, "Your context may be released...");
                    forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                    return;
                }

                if (NendUnifiedNativeAdMapper.canDownloadImage(
                    context, nendAdNative.getLogoImageUrl())) {
                    nendAdNative.downloadLogoImage(new NendAdNative.Callback() {
                        @Override
                        public void onSuccess(Bitmap logoImageBitmap) {
                            mappingUnifiedNativeNormalAd(nendAdNative, false, adImageBitmap,
                                logoImageBitmap);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            mappingUnifiedNativeNormalAd(nendAdNative, adImageBitmap);
                        }
                    });
                } else {
                    mappingUnifiedNativeNormalAd(nendAdNative, adImageBitmap);
                }
            }

            @Override
            public void onFailure(Exception e) {
                forwarder.failedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        });
    }

    NativeAdLoader(NendNativeAdForwarder forwarder, NendAdNativeClient client,
            NativeAdOptions nativeAdOptions) {
        this.forwarder = forwarder;
        this.client = client;
        this.nativeAdOptions = nativeAdOptions;
    }

    void loadAd() {
        client.loadAd(normalAdLoaderCallback);
    }
}
