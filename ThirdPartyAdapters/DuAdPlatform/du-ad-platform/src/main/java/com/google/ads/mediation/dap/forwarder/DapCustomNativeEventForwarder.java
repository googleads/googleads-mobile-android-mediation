package com.google.ads.mediation.dap.forwarder;

import android.content.Context;

import com.duapps.ad.AdError;
import com.duapps.ad.DuAdListener;
import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAdMediation;
import com.google.ads.mediation.dap.DuNativeAdAdapter;
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

    public DapCustomNativeEventForwarder(Context context, MediationNativeAdapter adapter, MediationNativeListener listener,
                                         NativeMediationAdRequest mediationAdRequest) {
        mContext = context.getApplicationContext();
        mAdapter = adapter;
        mNativeListener = listener;
        mMediationAdRequest = mediationAdRequest;

    }

    @Override
    public void onError(DuNativeAd duNativeAd, AdError adError) {
        if (mNativeListener != null) {
            DuAdMediation.d(TAG, "Native onError - " + adError.getErrorMessage());
            mNativeListener.onAdFailedToLoad(mAdapter, adError.getErrorCode());
        }
    }

    @Override
    public void onAdLoaded(DuNativeAd duNativeAd) {
        DuAdMediation.d(TAG, "Native onAdLoaded " + duNativeAd.getTitle());
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
                    DuAdMediation.d(TAG, "onMappingSuccess ");

                }
            }

            @Override
            public void onMappingFailed() {
                if (mNativeListener != null) {
                    DuAdMediation.d(TAG, "onMappingFailed ");
                    mNativeListener.onAdFailedToLoad(mAdapter, 5);
                }
            }
        });


    }

    @Override
    public void onClick(DuNativeAd duNativeAd) {
        if (mNativeListener != null) {
            DuAdMediation.d(TAG, "Dap NativeAd clicked.");
            mNativeListener.onAdClicked(mAdapter);
            mNativeListener.onAdOpened(mAdapter);
            mNativeListener.onAdLeftApplication(mAdapter);
        }
    }
}
