package com.google.ads.mediation.dap.forwarder;

import com.duapps.ad.AdError;
import com.duapps.ad.DuAdListener;
import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.DuAd;
import com.google.ads.mediation.dap.DuNativeAdAdapter;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;

public class DapCustomNativeEventForwarder implements DuAdListener {
    private static final String TAG = DuNativeAdAdapter.class.getSimpleName();
    private final MediationNativeListener mNativeListener;
    private final MediationNativeAdapter mAdapter;

    public DapCustomNativeEventForwarder(MediationNativeAdapter adapter, MediationNativeListener listener) {
        mAdapter = adapter;
        mNativeListener = listener;

    }

    @Override
    public void onError(DuNativeAd duNativeAd, AdError adError) {
        if (mNativeListener != null) {
            DuAd.d(TAG, "Native onError - " + adError.getErrorMessage());
            mNativeListener.onAdFailedToLoad(mAdapter, adError.getErrorCode());
        }
    }

    @Override
    public void onAdLoaded(DuNativeAd duNativeAd) {
        DuAd.d(TAG, "Native onAdLoaded " + duNativeAd.getTitle());

        final DuNativeAdMapper mapper = new DuNativeAdMapper(duNativeAd);
        mapper.mapNativeAd(new DuNativeAdMapper.NativeAdMapperListener() {
            @Override
            public void onMappingSuccess() {
                if (mNativeListener != null) {
                    mNativeListener.onAdLoaded(mAdapter, mapper);
                    DuAd.d(TAG, "onMappingSuccess ");

                }
            }

            @Override
            public void onMappingFailed() {
                if (mNativeListener != null) {
                    DuAd.d(TAG, "onMappingFailed ");
                    mNativeListener.onAdFailedToLoad(mAdapter, 5);
                }
            }
        });


    }

    @Override
    public void onClick(DuNativeAd duNativeAd) {
        if (mNativeListener != null) {
            DuAd.d(TAG, "Dap NativeAd clicked.");
            mNativeListener.onAdClicked(mAdapter);
        }
    }
}
