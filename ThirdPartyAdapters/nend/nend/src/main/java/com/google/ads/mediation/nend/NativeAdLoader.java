package com.google.ads.mediation.nend;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;

import net.nend.android.NendAdNative;
import net.nend.android.NendAdNativeClient;

import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

class NativeAdLoader {
    private NendNativeAdForwarder forwarder;

    private NendAdNativeClient client;
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

            if (!NendUnifiedNativeAdMapper.canDownloadImage(context, nendAdNative.getAdImageUrl())) {
                mappingUnifiedNativeNormalAd(nendAdNative, false,null, null);
            } else {
                if (nativeAdOptions != null && nativeAdOptions.shouldReturnUrlsForImageAssets()) {
                    mappingUnifiedNativeNormalAd(nendAdNative, true,null, null);
                } else {
                    downloadImageThenMappingUnifiedNativeNormalAd(nendAdNative);
                }
            }
        }

        @Override
        public void onFailure(NendAdNativeClient.NendError nendError) {
            forwarder.unifiedNativeAdMapper = null;
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

    private void mappingUnifiedNativeNormalAd(NendAdNative ad, Bitmap adImageBitmap) {
        mappingUnifiedNativeNormalAd(ad, false, adImageBitmap,null);
    }

    private void mappingUnifiedNativeNormalAd(NendAdNative ad, boolean shouldReturnUrlsForImageAssets, Bitmap adImageBitmap, Bitmap logoImageBitmap) {
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

        forwarder.unifiedNativeAdMapper =
                new NendUnifiedNativeNormalAdMapper(context, forwarder, ad, adImage, logoImage);
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

                if (!NendUnifiedNativeAdMapper.canDownloadImage(context, nendAdNative.getLogoImageUrl())) {
                    mappingUnifiedNativeNormalAd(nendAdNative, adImageBitmap);
                } else {
                    nendAdNative.downloadLogoImage(new NendAdNative.Callback() {
                        @Override
                        public void onSuccess(Bitmap logoImageBitmap) {
                            mappingUnifiedNativeNormalAd(nendAdNative, false, adImageBitmap, logoImageBitmap);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            mappingUnifiedNativeNormalAd(nendAdNative, adImageBitmap);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                forwarder.failedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        });
    }

    NativeAdLoader(
            NendNativeAdForwarder forwarder,
            NendAdNativeClient client,
            NativeAdOptions nativeAdOptions) {
        this.forwarder = forwarder;
        this.client = client;
        this.nativeAdOptions = nativeAdOptions;
    }

    void loadAd() {
        client.loadAd(normalAdLoaderCallback);
    }
}
