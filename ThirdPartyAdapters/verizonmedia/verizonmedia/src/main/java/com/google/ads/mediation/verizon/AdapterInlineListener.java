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

import java.lang.ref.WeakReference;
import java.util.Map;


final class AdapterInlineListener implements InlineAdView.InlineAdListener,
        InlineAdFactory.InlineAdFactoryListener {

    private static final String TAG = AdapterInlineListener.class.getSimpleName();
    private final LinearLayout adContainer;

    private WeakReference<MediationBannerAdapter> bannerAdapterWeakRef;
    private MediationBannerListener bannerListener;
    private InlineAdView inlineAdView;


    AdapterInlineListener(final MediationBannerAdapter adapter,
                          final MediationBannerListener listener,
                          final LinearLayout internalView) {

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

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                if ((bannerListener != null) && (adapter != null)) {
                    bannerListener.onAdOpened(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK inline ad expanded.");
    }


    @Override
    public void onCollapsed(final InlineAdView inlineAdView) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                if ((bannerListener != null) && (adapter != null)) {
                    bannerListener.onAdClosed(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK inline ad collapsed.");
    }


    @Override
    public void onClicked(final InlineAdView inlineAdView) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                if ((bannerListener != null) && (adapter != null)) {
                    bannerListener.onAdClicked(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK inline ad clicked.");

    }


    @Override
    public void onAdLeftApplication(final InlineAdView inlineAdView) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                if ((bannerListener != null) && (adapter != null)) {
                    bannerListener.onAdLeftApplication(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK inline ad left application.");
    }


    @Override
    public void onAdRefreshed(final InlineAdView inlineAdView) {
        // no op.  refreshing not supported in adapter
    }


    @Override
    public void onEvent(final InlineAdView inlineAdView, final String s, final String s1,
                        final Map<String, Object> map) {
        // no op.  events not supported in adapter
    }


    @Override
    public void onLoaded(final InlineAdFactory inlineAdFactory, final InlineAdView inlineAdView) {

        this.inlineAdView = inlineAdView;

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                adContainer.addView(inlineAdView);

                if ((bannerListener != null) && (adapter != null)) {
                    bannerListener.onAdLoaded(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK inline ad request succeeded.");
    }


    @Override
    public void onCacheLoaded(final InlineAdFactory inlineAdFactory, final int i, final int i1) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onCacheUpdated(final InlineAdFactory inlineAdFactory, final int i) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onError(final InlineAdFactory inlineAdFactory, final ErrorInfo errorInfo) {

        Log.i(TAG, "Verizon Ads SDK Inline Ad request failed (" + errorInfo.getErrorCode() + "): " +
                errorInfo.getDescription());

        switch (errorInfo.getErrorCode()) {
            case VASAds.ERROR_AD_REQUEST_FAILED:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                        if ((bannerListener != null) && (adapter != null)) {
                            bannerListener.onAdFailedToLoad(adapter,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                });
                break;
            case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                        if ((bannerListener != null) && (adapter != null)) {
                            bannerListener.onAdFailedToLoad(adapter,
                                    AdRequest.ERROR_CODE_NETWORK_ERROR);
                        }
                    }
                });
                break;
            default:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationBannerAdapter adapter = bannerAdapterWeakRef.get();

                        if ((bannerListener != null) && (adapter != null)) {
                            bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NO_FILL);
                        }
                    }
                });
        }
    }


    void destroy() {

        if (inlineAdView != null) {
            inlineAdView.destroy();
        }
    }
}
