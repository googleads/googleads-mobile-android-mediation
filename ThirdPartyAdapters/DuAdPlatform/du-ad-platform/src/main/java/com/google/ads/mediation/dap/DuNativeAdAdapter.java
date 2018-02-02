package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.forwarder.DapCustomNativeEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuNativeAdAdapter implements MediationNativeAdapter {
    private static final String TAG = DuNativeAdAdapter.class.getSimpleName();
    private DuNativeAd nativeAd;
    public static final String KEY_SOURCE = "source";

    @Override
    public void requestNativeAd(Context context,
                                MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        if (context == null) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        DuAdAdapter.initializeSDK(context, mediationExtras, pid);
        nativeAd = new DuNativeAd(context, pid);
        nativeAd.setMobulaAdListener(new DapCustomNativeEventForwarder(DuNativeAdAdapter.this, listener));
        nativeAd.load();
    }

    @Override
    public void onDestroy() {
        DuAdAdapter.d(TAG, "DuNativeAdAdapter onDestroy ");
        if (nativeAd != null) {
            nativeAd.destory();
            nativeAd = null;
        }
    }

    @Override
    public void onPause() {
        DuAdAdapter.d(TAG, "DuNativeAdAdapter onPause");
    }

    @Override
    public void onResume() {
        DuAdAdapter.d(TAG, "DuNativeAdAdapter onResume");
    }


    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DuAdAdapter.KEY_DAP_PID);
        if (TextUtils.isEmpty(pidStr)) {
            return -1;
        }
        int pid = -1;
        try {
            pid = Integer.parseInt(pidStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
        if (pid < 0) {
            return -1;
        }
        return pid;
    }
}
