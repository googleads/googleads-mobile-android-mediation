package com.google.ads.mediation.dap.forwarder;

import android.content.Context;

import com.duapps.ad.AdError;
import com.duapps.ad.DuAdListener;
import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.ads.mediation.dap.DuNativeAdAdapter;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

public class DapCustomNativeEventForwarder implements DuAdListener {

    private static final String TAG = DuNativeAdAdapter.class.getSimpleName();
    private final MediationNativeListener mNativeListener;
    private final MediationNativeAdapter mAdapter;
    private final NativeMediationAdRequest mMediationAdRequest;
    private final Context mContext;

    public DapCustomNativeEventForwarder(Context context,
                                         MediationNativeAdapter adapter,
                                         MediationNativeListener listener,
                                         NativeMediationAdRequest mediationAdRequest) {
        mContext = context.getApplicationContext();
        mAdapter = adapter;
        mNativeListener = listener;
        mMediationAdRequest = mediationAdRequest;
    }

    @Override
    public void onError(DuNativeAd duNativeAd, AdError adError) {
        if (mNativeListener != null) {
            DuAdMediation.debugLog(TAG, "Native onError - " + adError.getErrorMessage());
            mNativeListener.onAdFailedToLoad(mAdapter, getAdMobErrorCode(adError.getErrorCode()));
        }
    }

    @Override
    public void onAdLoaded(DuNativeAd duNativeAd) {
        DuAdMediation.debugLog(TAG, "Native onAdLoaded " + duNativeAd.getTitle());
        NativeAdOptions nativeAdOptions = null;
        if (mMediationAdRequest != null) {
            nativeAdOptions = mMediationAdRequest.getNativeAdOptions();
        }

        final DuNativeAdMapper mapper = new DuNativeAdMapper(mContext,duNativeAd, nativeAdOptions);
        mapper.mapNativeAd(new DuNativeAdMapper.NativeAdMapperListener() {
            @Override
            public void onMappingSuccess() {
                if (mNativeListener != null) {
                    mNativeListener.onAdLoaded(mAdapter, mapper);
                    DuAdMediation.debugLog(TAG, "onMappingSuccess ");
                }
            }

            @Override
            public void onMappingFailed() {
                if (mNativeListener != null) {
                    DuAdMediation.debugLog(TAG, "onMappingFailed ");
                    mNativeListener.onAdFailedToLoad(mAdapter, 5);
                }
            }
        });
    }

    @Override
    public void onClick(DuNativeAd duNativeAd) {
        if (mNativeListener != null) {
            DuAdMediation.debugLog(TAG, "Dap NativeAd clicked.");
            mNativeListener.onAdClicked(mAdapter);
            mNativeListener.onAdOpened(mAdapter);
            mNativeListener.onAdLeftApplication(mAdapter);
        }
    }

    private int getAdMobErrorCode(int duAdErrorCode){
        switch (duAdErrorCode){
            case 2000: // SERVER_ERROR_CODE: Server Error
            case 2001: // INTERNAL_ERROR_CODE: Network Error
            case 3001: // UNKNOWN_ERROR_CODE: Unknown Error
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
            case 1002: // LOAD_TOO_FREQUENTLY_ERROR_CODE: Too many interface requests
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case 1000: // NETWORK_ERROR_CODE: Client network error
            case 3000: // TIME_OUT_CODE: Retrieve Ad data timed out
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case 1001: // NO_FILL_ERROR_CODE: No Ad data retrieved
            case 1003: // IMPRESSION_LIMIT_ERROR_CODE: Reach the daily impression limit
                return AdRequest.ERROR_CODE_NO_FILL;
            default:
        }
        return AdRequest.ERROR_CODE_INTERNAL_ERROR;
    }
}
