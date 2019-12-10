package com.google.ads.mediation.verizon;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.verizon.ads.Component;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.NativeAdFactory;
import com.verizon.ads.utils.TextUtils;
import com.verizon.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;
import static com.google.ads.mediation.verizon.VerizonMediationAdapter.initializeSDK;

final class VerizonMediaNativeRenderer implements NativeAd.NativeAdListener,
        NativeAdFactory.NativeAdFactoryListener {

    /**
     * The mediation native adapter weak reference.
     */
    private final WeakReference<MediationNativeAdapter> nativeAdapterWeakRef;

    /**
     * The mediation native listener used to report native ad event callbacks.
     */
    private  MediationNativeListener nativeListener;

    /**
     * The Context.
     */
    private  Context context;
    /**
     * Verizon Media native ad.
     */
    private NativeAd nativeAd;

    public VerizonMediaNativeRenderer(MediationNativeAdapter adapter) {
        this.nativeAdapterWeakRef = new WeakReference<>(adapter);
    }

    public void render(@NonNull Context context, MediationNativeListener listener,
            Bundle serverParameters, NativeMediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {
        nativeListener = listener;
        this.context = context;
        String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters, mediationExtras);
        MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

        if (TextUtils.isEmpty(siteId)) {
            Log.e(TAG, "Failed to request ad: siteID is null.");
            if (nativeListener != null && adapter != null) {
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
                return;
            }
        }

        if (!initializeSDK(context, siteId)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK.");
            if (nativeListener != null && adapter != null) {
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
            return;
        }

        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        if (TextUtils.isEmpty(placementId)) {
            Log.e(TAG, "Failed to request ad: placementID is null or empty.");
            if (nativeListener != null && adapter != null) {
                nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        VerizonMediaAdapterUtils.setCoppaValue(mediationAdRequest);
        VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
        String[] adTypes = new String[] {"inline"};
        NativeAdFactory nativeAdFactory = new NativeAdFactory(context, placementId, adTypes, this);
        nativeAdFactory.setRequestMetaData(
                VerizonMediaAdapterUtils.getRequestMetadata(mediationAdRequest));
        NativeAdOptions options = mediationAdRequest.getNativeAdOptions();

        if ((options == null) || (!options.shouldReturnUrlsForImageAssets())) {
            nativeAdFactory.load(this);
        } else {
            nativeAdFactory.loadWithoutAssets(this);
        }
    }

    @Override
    public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {
        // This error callback is used if the native ad is loaded successfully, but an error
        // occurs while trying to display a component
        Log.e(TAG, "Verizon Ads SDK native ad error: " + errorInfo);
    }

    @Override
    public void onClosed(final NativeAd nativeAd) {
        Log.i(TAG, "Verizon Ads SDK native ad closed.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
                if (nativeListener != null && adapter != null) {
                    nativeListener.onAdClosed(adapter);
                }
            }
        });
    }

    @Override
    public void onClicked(final NativeAd nativeAd, final Component component) {
        Log.i(TAG, "Verizon Ads SDK native ad clicked.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
                if (nativeListener != null && adapter != null) {
                    nativeListener.onAdOpened(adapter);
                    nativeListener.onAdClicked(adapter);
                }
            }
        });
    }

    @Override
    public void onAdLeftApplication(final NativeAd nativeAd) {
        Log.i(TAG, "Verizon Ads SDK native ad left application.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
                if (nativeListener != null && adapter != null) {
                    nativeListener.onAdLeftApplication(adapter);
                }
            }
        });
    }

    @Override
    public void onEvent(final NativeAd nativeAd, final String source, final String eventId,
            final Map<String, Object> arguments) {

        // no op.  events not supported in adapter
    }

    @Override
    public void onLoaded(final NativeAdFactory nativeAdFactory, final NativeAd nativeAd) {
        this.nativeAd = nativeAd;
        Log.i(TAG, "Verizon Ads SDK native ad request succeeded: Loading succeeded.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                final MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
                final AdapterUnifiedNativeAdMapper mapper =
                        new AdapterUnifiedNativeAdMapper(context, nativeAd);

                mapper.loadResources(new AdapterUnifiedNativeAdMapper.LoadListener() {
                    @Override
                    public void onLoadComplete() {
                        ThreadUtils.postOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nativeListener.onAdLoaded(adapter, mapper);
                            }
                        });
                    }

                    @Override
                    public void onLoadError() {
                        ThreadUtils.postOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nativeListener.onAdFailedToLoad(adapter,
                                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onCacheLoaded(final NativeAdFactory nativeAdFactory, final int numRequested,
            final int numReceived) {

        // no op.  caching not supported in adapter
    }

    @Override
    public void onCacheUpdated(final NativeAdFactory nativeAdFactory, final int cacheSize) {

        // no op.  caching not supported in adapter
    }

    @Override
    public void onError(final NativeAdFactory nativeAdFactory, final ErrorInfo errorInfo) {
        Log.i(TAG, "Verizon Ads SDK Native Ad request failed (" + errorInfo.getErrorCode() + "): " +
                errorInfo.getDescription());
        final int errorCode;
        switch (errorInfo.getErrorCode()) {
            case VASAds.ERROR_AD_REQUEST_FAILED:
                errorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                break;
            case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
                errorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                break;
            default:
                errorCode = AdRequest.ERROR_CODE_NO_FILL;
        }
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationNativeAdapter adapter = nativeAdapterWeakRef.get();
                if (nativeListener != null && adapter != null) {
                    nativeListener.onAdFailedToLoad(adapter, errorCode);
                }
            }
        });
    }

    void destroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
    }
}
