package com.google.ads.mediation.verizon;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.verizon.ads.Component;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.NativeAdFactory;
import com.verizon.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;

final class AdapterNativeListener implements NativeAd.NativeAdListener,
        NativeAdFactory.NativeAdFactoryListener {
    /**
     * The mediation native adapter weak reference.
     */
    private final WeakReference<MediationNativeAdapter> nativeAdapterWeakRef;
    /**
     * The mediation native listener used to report native ad event callbacks.
     */
    private final MediationNativeListener nativeListener;
    /**
     * The Context.
     */
    private final Context context;
    /**
     * Verizon Media native ad.
     */
    private NativeAd nativeAd;

    public AdapterNativeListener(final Context context, final MediationNativeAdapter adapter,
            final MediationNativeListener listener) {
        this.nativeAdapterWeakRef = new WeakReference<>(adapter);
        this.nativeListener = listener;
        this.context = context;
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
