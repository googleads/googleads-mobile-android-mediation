package com.google.ads.mediation.nend;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

import net.nend.android.NendAdNativeClient;

import java.lang.ref.WeakReference;

/*
 * The {@link NendNativeAdForwarder} to load and show Nend native ads, Nend native video ads.
 */
class NendNativeAdForwarder {
    private static final String TAG = NendNativeAdForwarder.class.getSimpleName();

    private NendMediationAdapter adapter;
    private MediationNativeListener mediationNativeListener;
    WeakReference<Context> contextWeakReference;

    static final String KEY_NATIVE_ADS_FORMAT_TYPE = "key_native_ads_format_type";

    private NativeAdLoader normalAdLoader;
    NendUnifiedNativeAdMapper unifiedNativeAdMapper;
    private NativeVideoAdLoader videoAdLoader;

    NendNativeAdForwarder(NendMediationAdapter adapter) {
        this.adapter = adapter;
    }

    void adLoaded() {
        mediationNativeListener.onAdLoaded(adapter, unifiedNativeAdMapper);
    }

    void failedToLoad(int nendErrorCode) {
        int errorCode = ErrorUtil.convertErrorCodeFromNendVideoToAdMob(nendErrorCode);
        Log.w(TAG, "Failed to request ad from Nend, Error Code: " + errorCode);
        mediationNativeListener.onAdFailedToLoad(adapter, errorCode);
    }

    void adImpression() {
        mediationNativeListener.onAdImpression(adapter);
    }

    void adClicked() {
        mediationNativeListener.onAdClicked(adapter);
    }

    void informationClicked() {
        mediationNativeListener.onAdLeftApplication(adapter);
    }

    void adOpened() {
        mediationNativeListener.onAdOpened(adapter);
    }

    void adClosed() {
        mediationNativeListener.onAdClosed(adapter);
    }

    void leftApplication() {
        mediationNativeListener.onAdLeftApplication(adapter);
    }

    void endVideo() {
        mediationNativeListener.onVideoEnd(adapter);
    }

    void onResume() {
        // Do nothing here
    }

    void onPause() {
        // Do nothing here
    }

    void onDestroy() {
        mediationNativeListener = null;
        normalAdLoader = null;
        if (unifiedNativeAdMapper instanceof NendUnifiedNativeVideoAdMapper) {
            ((NendUnifiedNativeVideoAdMapper)unifiedNativeAdMapper).deactivate();
            unifiedNativeAdMapper = null;
        }
        if (videoAdLoader != null) {
            videoAdLoader.releaseLoader();
            videoAdLoader = null;
        }
        if (contextWeakReference != null) {
            contextWeakReference.clear();
            contextWeakReference = null;
        }
    }

    void requestNativeAd(
            Context context,
            MediationNativeListener mediationNativeListener,
            Bundle serverParameters,
            NativeMediationAdRequest nativeMediationAdRequest,
            Bundle mediationExtras) {
        if (!nativeMediationAdRequest.isUnifiedNativeAdRequested()) {
            Log.e(TAG, "Failed to load ad. Request must be for unified native ads.");
            mediationNativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        AdUnitMapper mapper = AdUnitMapper.validateNendAdUnit(serverParameters);
        if (mapper == null) {
            mediationNativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        contextWeakReference = new WeakReference<>(context);
        this.mediationNativeListener = mediationNativeListener;

        if (mediationExtras != null) {
            final NendMediationAdapter.FormatType type = (NendMediationAdapter.FormatType) mediationExtras.getSerializable(KEY_NATIVE_ADS_FORMAT_TYPE);
            if (type != null) {
                switch (type) {
                    case TYPE_VIDEO:
                        videoAdLoader = new NativeVideoAdLoader(
                                this, mapper, nativeMediationAdRequest, mediationExtras);
                        videoAdLoader.loadAd();
                        return;
                    case TYPE_NORMAL:
                        break;
                    default:
                        Log.w(TAG, "Unknown type: "+ type);
                        break;
                }
            }
        }

        normalAdLoader = new NativeAdLoader(
                this,
                new NendAdNativeClient(context, mapper.spotId, mapper.apiKey),
                nativeMediationAdRequest.getNativeAdOptions());
        normalAdLoader.loadAd();
    }
}
