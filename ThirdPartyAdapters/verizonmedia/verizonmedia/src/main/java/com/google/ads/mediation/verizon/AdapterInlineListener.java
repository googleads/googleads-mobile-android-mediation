package com.google.ads.mediation.verizon;

import android.util.Log;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.inlineplacement.InlineAdFactory;
import com.verizon.ads.inlineplacement.InlineAdView;
import com.verizon.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;

final class AdapterInlineListener implements InlineAdView.InlineAdListener,
        InlineAdFactory.InlineAdFactoryListener {

    /**
     * The ad view's container.
     */
    private final LinearLayout adContainer;
    /**
     * The mediation banner adapter weak reference.
     */
    private WeakReference<MediationBannerAdapter> bannerAdapterWeakRef;
    /**
     * The mediation banner listener used to report banner ad event callbacks.
     */
    private MediationBannerListener bannerListener;
    /**
     * Verizon Media ad view.
     */
    private InlineAdView inlineAdView;

    public AdapterInlineListener(final MediationBannerAdapter adapter,
            final MediationBannerListener listener, final LinearLayout internalView) {

        bannerAdapterWeakRef = new WeakReference<>(adapter);
        bannerListener = listener;
        adContainer = internalView;
    }

    @Override
    public void onError(final InlineAdView inlineAdView, final ErrorInfo errorInfo) {
        Log.e(TAG, "Verizon Ads SDK inline ad error: " + errorInfo);
    }

    @Override
    public void onResized(final InlineAdView inlineAdView) {
        Log.d(TAG, "Verizon Ads SDK on resized");
    }

    @Override
    public void onExpanded(final InlineAdView inlineAdView) {
        Log.i(TAG, "Verizon Ads SDK inline ad expanded.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdOpened(adapter);
                }
            }
        });
    }

    @Override
    public void onCollapsed(final InlineAdView inlineAdView) {
        Log.i(TAG, "Verizon Ads SDK inline ad collapsed.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdClosed(adapter);
                }
            }
        });
    }

    @Override
    public void onClicked(final InlineAdView inlineAdView) {
        Log.i(TAG, "Verizon Ads SDK inline ad clicked.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdClicked(adapter);
                }
            }
        });
    }

    @Override
    public void onAdLeftApplication(final InlineAdView inlineAdView) {
        Log.i(TAG, "Verizon Ads SDK inline ad left application.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdLeftApplication(adapter);
                }
            }
        });
    }

    @Override
    public void onAdRefreshed(final InlineAdView inlineAdView) {
        // no op.  refreshing not supported in adapter
    }

    @Override
    public void onEvent(final InlineAdView inlineAdView, final String source, final String eventId,
            final Map<String, Object> arguments) {
        // no op.  events not supported in adapter
    }

    @Override
    public void onLoaded(final InlineAdFactory inlineAdFactory, final InlineAdView inlineAdView) {
        this.inlineAdView = inlineAdView;
        Log.i(TAG, "Verizon Ads SDK inline ad request succeeded.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                adContainer.addView(inlineAdView);
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdLoaded(adapter);
                }
            }
        });
    }

    @Override
    public void onCacheLoaded(final InlineAdFactory inlineAdFactory, final int numRequested,
            final int numReceived) {
        // no op.  caching not supported in adapter
    }

    @Override
    public void onCacheUpdated(final InlineAdFactory inlineAdFactory, final int cacheSize) {
        // no op.  caching not supported in adapter
    }

    @Override
    public void onError(final InlineAdFactory inlineAdFactory, final ErrorInfo errorInfo) {
        Log.i(TAG, "Verizon Ads SDK Inline Ad request failed (" + errorInfo.getErrorCode() + "): " +
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
                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();
                if (bannerListener != null && adapter != null) {
                    bannerListener.onAdFailedToLoad(adapter, errorCode);
                }
            }
        });
    }

    void destroy() {
        if (inlineAdView != null) {
            inlineAdView.destroy();
        }
    }
}
