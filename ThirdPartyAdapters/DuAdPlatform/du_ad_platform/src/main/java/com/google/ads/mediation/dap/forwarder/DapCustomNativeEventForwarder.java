package com.google.ads.mediation.dap.forwarder;

import android.util.Log;

import com.dap.mediation.customevent.DuNativeAdMapper;
import com.duapps.ad.AdError;
import com.duapps.ad.DuAdListener;
import com.duapps.ad.DuNativeAd;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;

public class DapCustomNativeEventForwarder implements DuAdListener {
    private static final String TAG = "DapNativeCustomEvent";
    private final MediationNativeListener mNativeListener;
    private final MediationNativeAdapter mAdapter;

    public DapCustomNativeEventForwarder(MediationNativeAdapter adapter, MediationNativeListener listener) {
        mAdapter = adapter;
        mNativeListener = listener;

    }

    @Override
    public void onError(DuNativeAd duNativeAd, AdError adError) {
        if (mNativeListener != null) {
            Log.d(TAG, "Native onError - " + adError.getErrorMessage());
            mNativeListener.onAdFailedToLoad(mAdapter, adError.getErrorCode());
        }
    }

    @Override
    public void onAdLoaded(DuNativeAd duNativeAd) {
        Log.d(TAG, "Native onAdLoaded " + duNativeAd.getTitle());

        final DuNativeAdMapper mapper = new DuNativeAdMapper(duNativeAd);
        mapper.mapNativeAd(new DuNativeAdMapper.NativeAdMapperListener() {
            @Override
            public void onMappingSuccess() {
                if (mNativeListener != null) {
                    mNativeListener.onAdLoaded(mAdapter, mapper);
                    Log.d(TAG, "onMappingSuccess ");

                }
            }

            @Override
            public void onMappingFailed() {
                if (mNativeListener != null) {
                    Log.d(TAG, "onMappingFailed ");
                    mNativeListener.onAdFailedToLoad(mAdapter, 5);
                }
            }
        });


    }

    @Override
    public void onClick(DuNativeAd duNativeAd) {
        if (mNativeListener != null) {
            Log.d(TAG, "Dap NativeAd clicked.");
            mNativeListener.onAdClicked(mAdapter);
        }
    }
}
