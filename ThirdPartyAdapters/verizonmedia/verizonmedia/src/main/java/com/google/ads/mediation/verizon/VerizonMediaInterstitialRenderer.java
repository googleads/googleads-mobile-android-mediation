package com.google.ads.mediation.verizon;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;
import com.verizon.ads.utils.TextUtils;
import com.verizon.ads.utils.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;
import static com.google.ads.mediation.verizon.VerizonMediationAdapter.initializeSDK;

final class VerizonMediaInterstitialRenderer implements InterstitialAd.InterstitialAdListener,
        InterstitialAdFactory.InterstitialAdFactoryListener {

    /**
     * The mediation interstitial adapter weak reference.
     */
    private WeakReference<MediationInterstitialAdapter> interstitialAdapterWeakRef;

    /**
     * The mediation interstitial listener used to report interstitial ad event callbacks.
     */
    private MediationInterstitialListener interstitialListener;

    /**
     * Verizon Media interstitial ad.
     */
    private InterstitialAd interstitialAd;

    public VerizonMediaInterstitialRenderer(final MediationInterstitialAdapter adapter) {
        interstitialAdapterWeakRef = new WeakReference<>(adapter);
    }

    public void render(@NonNull Context context, MediationInterstitialListener listener,
            MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle mediationExtras)
    {
        interstitialListener = listener;
        String siteId = VerizonMediaAdapterUtils.getSiteId(serverParameters, mediationExtras);
        MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
        if (TextUtils.isEmpty(siteId)) {
            Log.e(TAG, "Failed to request ad: siteID is null or empty.");
            if (interstitialListener != null && adapter != null) {
                interstitialListener.onAdFailedToLoad(adapter,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!initializeSDK(context, siteId)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK.");
            if (interstitialListener != null && adapter != null) {
                interstitialListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
            return;
        }

        String placementId = VerizonMediaAdapterUtils.getPlacementId(serverParameters);
        if (TextUtils.isEmpty(placementId)) {
            Log.e(TAG, "Failed to request ad: placementID is null or empty.");
            interstitialListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        VerizonMediaAdapterUtils.setCoppaValue(mediationAdRequest);
        VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
        InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(context, placementId
                , this);
        interstitialAdFactory.setRequestMetaData(VerizonMediaAdapterUtils
                .getRequestMetadata(mediationAdRequest));
        interstitialAdFactory.load(this);
    }

    @Override
    public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {
        Log.e(TAG, "Verizon Ads SDK interstitial error: " + errorInfo);
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdOpened(adapter);
                    interstitialListener.onAdClosed(adapter);
                }
            }
        });
    }

    @Override
    public void onShown(final InterstitialAd interstitialAd) {
        Log.i(TAG, "Verizon Ads SDK interstitial shown.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdOpened(adapter);
                }
            }
        });
    }

    @Override
    public void onClosed(final InterstitialAd interstitialAd) {
        Log.i(TAG, "Verizon Ads SDK ad closed");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdClosed(adapter);
                }
            }
        });
    }

    @Override
    public void onClicked(final InterstitialAd interstitialAd) {
        Log.i(TAG, "Verizon Ads SDK interstitial clicked.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdClicked(adapter);
                }
            }
        });
    }

    @Override
    public void onAdLeftApplication(final InterstitialAd interstitialAd) {
        Log.i(TAG, "Verizon Ads SDK interstitial left application.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdLeftApplication(adapter);
                }
            }
        });
    }

    @Override
    public void onEvent(final InterstitialAd interstitialAd, final String source,
            final String eventId, final Map<String, Object> arguments) {
        // no op.  events not supported in adapter
    }

    @Override
    public void onLoaded(final InterstitialAdFactory interstitialAdFactory,
            final InterstitialAd interstitialAd) {

        this.interstitialAd = interstitialAd;
        Log.i(TAG, "Verizon Ads SDK interstitial loaded.");
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();
                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdLoaded(adapter);
                }
            }
        });
    }

    @Override
    public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory,
            final int numRequested, final int numReceived) {
        // no op.  caching not supported in adapter
    }

    @Override
    public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory,
            final int cacheSize) {
        // no op.  caching not supported in adapter
    }

    @Override
    public void onError(final InterstitialAdFactory interstitialAdFactory,
            final ErrorInfo errorInfo) {
        Log.w(TAG, "Verizon Ads SDK interstitial request failed (" + errorInfo.getErrorCode()
                + "): " + errorInfo.getDescription());
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
                MediationInterstitialAdapter adapter = interstitialAdapterWeakRef.get();

                if (adapter != null && interstitialListener != null) {
                    interstitialListener.onAdFailedToLoad(adapter, errorCode);
                }
            }
        });
    }

    void showInterstitial(@NonNull Context context) {
        if (interstitialAd == null) {
            Log.e(TAG, "Failed to show: No ads to show.");
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
