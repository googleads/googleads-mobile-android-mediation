package com.google.ads.mediation.verizon;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.lang.ref.WeakReference;
import java.util.Map;


final class AdapterInterstitialListener implements InterstitialAd.InterstitialAdListener,
        InterstitialAdFactory.InterstitialAdFactoryListener {

    private static final String TAG = AdapterInterstitialListener.class.getSimpleName();

    private WeakReference<MediationInterstitialAdapter> interstitialAdapterWeakRef;
    private MediationInterstitialListener interstitialListener;
    private InterstitialAd interstitialAd;


    AdapterInterstitialListener(final MediationInterstitialAdapter adapter,
                                final MediationInterstitialListener listener) {

        interstitialAdapterWeakRef = new WeakReference<>(adapter);
        interstitialListener = listener;
    }


    @Override
    public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

        Log.e(TAG, "Verizon Ads SDK interstitial error: " + errorInfo);

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdOpened(adapter);
                    interstitialListener.onAdClosed(adapter);
                }
            }
        });
    }


    @Override
    public void onShown(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdOpened(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK interstitial shown.");
    }


    @Override
    public void onClosed(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdClosed(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK ad closed");
    }


    @Override
    public void onClicked(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdClicked(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK interstitial clicked.");
    }


    @Override
    public void onAdLeftApplication(final InterstitialAd interstitialAd) {

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdLeftApplication(adapter);
                }
            }
        });
        Log.i(TAG, "Verizon Ads SDK interstitial left application.");
    }


    @Override
    public void onEvent(final InterstitialAd interstitialAd, final String s, final String s1,
                        final Map<String, Object> map) {
        // no op.  events not supported in adapter
    }


    @Override
    public void onLoaded(final InterstitialAdFactory interstitialAdFactory,
                         final InterstitialAd interstitialAd) {

        this.interstitialAd = interstitialAd;

        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {

                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if ((adapter != null) && (interstitialListener != null)) {
                    interstitialListener.onAdLoaded(adapter);
                }
            }
        });

        Log.i(TAG, "Verizon Ads SDK interstitial loaded.");
    }


    @Override
    public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory, final int i,
                              final int i1) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int i) {
        // no op.  caching not supported in adapter
    }


    @Override
    public void onError(final InterstitialAdFactory interstitialAdFactory,
                        final ErrorInfo errorInfo) {

        switch (errorInfo.getErrorCode()) {
            case VASAds.ERROR_AD_REQUEST_FAILED:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                        if ((adapter != null) && (interstitialListener != null)) {
                            interstitialListener.onAdFailedToLoad(adapter,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                });
                break;
            case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                        if ((adapter != null) && (interstitialListener != null)) {
                            interstitialListener.onAdFailedToLoad(adapter,
                                    AdRequest.ERROR_CODE_NETWORK_ERROR);
                        }
                    }
                });
                break;

            default:
                ThreadUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                        if ((adapter != null) && (interstitialListener != null)) {
                            interstitialListener.onAdFailedToLoad(adapter,
                                    AdRequest.ERROR_CODE_NO_FILL);
                        }
                    }
                });
                break;
        }
        Log.w(TAG, "Verizon Ads SDK interstitial request failed (" + errorInfo.getErrorCode()
                + "): " + errorInfo.getDescription());
    }


    void show(final Context context) {

        if ((interstitialAd == null) || (context == null)) {
            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                    if ((adapter != null) && (interstitialListener != null)) {
                        interstitialListener.onAdFailedToLoad(adapter,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
            return;
        }

        interstitialAd.show(context);
    }


    void destroy() {

        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
    }
}
