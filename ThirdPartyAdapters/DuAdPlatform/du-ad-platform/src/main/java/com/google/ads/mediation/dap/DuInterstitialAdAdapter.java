package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;

import com.duapps.ad.InterstitialAd;
import com.google.ads.mediation.dap.forwarder.DapCustomInterstitialEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import java.io.Serializable;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuInterstitialAdAdapter implements MediationInterstitialAdapter {
    private static final String TAG = DuInterstitialAdAdapter.class.getSimpleName();
    public static final String KEY_INTERSTITIAL_TYPE = "INTERSTITIAL_TYPE";
    private InterstitialAd mInterstitial;


    /* ****************** MediationInterstitialAdapter ********************** */

    @Override
    public void requestInterstitialAd(
            Context context,
            MediationInterstitialListener listener,
            Bundle serverParameters,
            MediationAdRequest mediationAdRequest,
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
        DuAdAdapter.d(TAG, "requestInterstitialAd " + ",pid = " + pid + ",omInterstitial" + mInterstitial);
        InterstitialAdType type = (InterstitialAdType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
        mInterstitial = new InterstitialAd(context, pid, getType(type));
        mInterstitial.setInterstitialListener(new DapCustomInterstitialEventForwarder(DuInterstitialAdAdapter.this, listener));
        mInterstitial.load();
    }

    private InterstitialAd.Type getType(InterstitialAdType type) {
        if (type == null) {
            return InterstitialAd.Type.SCREEN;
        }
        if (type == InterstitialAdType.NORMAL) {
            return InterstitialAd.Type.NORMAL;
        }
        return InterstitialAd.Type.SCREEN;
    }

    @Override
    public void showInterstitial() {
        if (mInterstitial != null) {
            DuAdAdapter.d(TAG, "showInterstitial ");
            mInterstitial.show();
        }
    }


    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        DuAdAdapter.d(TAG, "onDestroy ");
        if (mInterstitial != null) {
            mInterstitial.destory();
            mInterstitial = null;
        }
    }

    @Override
    public void onPause() {
        DuAdAdapter.d(TAG, "DuAdAdapter onPause");
    }

    @Override
    public void onResume() {
        DuAdAdapter.d(TAG, "DuAdAdapter onResume");
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

    public enum InterstitialAdType implements Serializable {
        NORMAL,
        SCREEN;
    }

    public static class ExtrasBundleBuilder {

        private InterstitialAdType interstitialAdType;

        public ExtrasBundleBuilder interstitialAdType(InterstitialAdType interstitialAdType) {
            this.interstitialAdType = interstitialAdType;
            return ExtrasBundleBuilder.this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_INTERSTITIAL_TYPE, interstitialAdType);
            return bundle;
        }
    }
}
