package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.DuNativeAd;
import com.google.ads.mediation.dap.forwarder.DapCustomNativeEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

/**
 * Mediation adapter for Native Ads for DU Ad Platform
 */
@Keep
public class DuNativeAdAdapter implements MediationNativeAdapter {
    private static final String TAG = DuNativeAdAdapter.class.getSimpleName();
    public static final String KEY_SOURCE = "source";
    private DuNativeAd nativeAd;

    // region MediationNativeAdapter implementation
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

        if (!(mediationAdRequest.isAppInstallAdRequested()
                && mediationAdRequest.isContentAdRequested())) {
            Log.w(TAG, "Failed to request native ad. "
                    + "Both app install and content ad should be requested");
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        int pid = getValidPid(serverParameters);
        String appId = serverParameters.getString(DuAdMediation.KEY_APP_ID);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        DuAdMediation.configureSDKForNonVideo(context, mediationExtras, appId, pid);
        nativeAd = new DuNativeAd(context, pid);
        nativeAd.setMobulaAdListener(new DapCustomNativeEventForwarder(
                context,DuNativeAdAdapter.this, listener, mediationAdRequest));
        nativeAd.load();
    }

    @Override
    public void onDestroy() {
        DuAdMediation.debugLog(TAG, "DuNativeAdAdapter onDestroy");
        if (nativeAd != null) {
            nativeAd.destory();
            nativeAd = null;
        }
    }

    @Override
    public void onPause() {
        DuAdMediation.debugLog(TAG, "DuNativeAdAdapter onPause");
    }

    @Override
    public void onResume() {
        DuAdMediation.debugLog(TAG, "DuNativeAdAdapter onResume");
    }
    // endregion

    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DuAdMediation.KEY_DAP_PID);
        if (TextUtils.isEmpty(pidStr)) {
            return -1;
        }
        int pid;
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
