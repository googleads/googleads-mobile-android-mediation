package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.duapps.ad.InterstitialAd;
import com.duapps.ad.banner.BannerAdView;
import com.duapps.ad.banner.BannerCloseStyle;
import com.duapps.ad.banner.BannerStyle;
import com.google.ads.mediation.dap.forwarder.DapCustomBannerEventForwarder;
import com.google.ads.mediation.dap.forwarder.DapCustomInterstitialEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * Mediation adapter for DU Ad Platform
 */
@Keep
public class DuAdAdapter
        implements MediationBannerAdapter, MediationInterstitialAdapter {

    private static final String TAG = DuAdAdapter.class.getSimpleName();

    // region MediationBannerAdapter implementation
    public static final String KEY_BANNER_STYLE = "BANNER_STYLE";
    public static final String KEY_BANNER_CLOSE_STYLE = "BANNER_CLOSE_STYLE";

    private BannerAdView mBannerAdView;

    @Override
    public void requestBannerAd(
            Context context,
            final MediationBannerListener listener,
            Bundle serverParameters,
            AdSize adSize,
            MediationAdRequest mediationAdRequest,
            Bundle mediationExtras) {
        if (context == null) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        int pid = getValidPid(serverParameters);
        String appId = serverParameters.getString(DuAdMediation.KEY_APP_ID);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        DuAdMediation.initializeSDK(context, mediationExtras, pid, appId);
        if (!DuAdMediation.checkClassExist("com.duapps.ad.banner.BannerAdView")) {
            String message = "The version of the Du Ad SDK included in this app does not include support for " +
                    "Banner ads. Please make sure that you are using the latest version of SDK";
            Log.e(TAG, message);
            Log.e(TAG, message);
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        DuAdMediation.d(TAG, "requestBannerAd" + ",pid = " + pid);
        mBannerAdView = new BannerAdView(context, pid, 5,
                new DapCustomBannerEventForwarder(DuAdAdapter.this, listener));
        DuAdExtrasBundleBuilder.BannerStyle bannerStyle = null;
        DuAdExtrasBundleBuilder.BannerCloseStyle bannerCloseStyle = null;
        if (mediationExtras != null) {
            bannerStyle = (DuAdExtrasBundleBuilder.BannerStyle) mediationExtras
                    .getSerializable(KEY_BANNER_STYLE);
            bannerCloseStyle = (DuAdExtrasBundleBuilder.BannerCloseStyle) mediationExtras
                    .getSerializable(KEY_BANNER_CLOSE_STYLE);
        }

        mBannerAdView.setBgStyle(getStyle(bannerStyle));
        mBannerAdView.setCloseStyle(getCloseStyle(bannerCloseStyle));
        mBannerAdView.load();
    }

    @Override
    public View getBannerView() {
        if (!DuAdMediation.checkClassExist("com.duapps.ad.banner.BannerAdView")) {
            String message = "The version of the Du Ad SDK included in this app does not include support for " +
                    "Banner ads. Please make sure that you are using the latest version of SDK";
            Log.e(TAG, message);
            Log.e(TAG, message);
            return null;
        }
        return mBannerAdView;
    }

    private BannerStyle getStyle(DuAdExtrasBundleBuilder.BannerStyle style) {
        if (style == null) {
            return BannerStyle.STYLE_GREEN;
        }
        if (style == DuAdExtrasBundleBuilder.BannerStyle.STYLE_BLUE) {
            return BannerStyle.STYLE_BLUE;
        }
        return BannerStyle.STYLE_GREEN;
    }

    private BannerCloseStyle getCloseStyle(DuAdExtrasBundleBuilder.BannerCloseStyle style) {
        if (style == null) {
            return BannerCloseStyle.STYLE_TOP;
        }
        if (style == DuAdExtrasBundleBuilder.BannerCloseStyle.STYLE_BOTTOM) {
            return BannerCloseStyle.STYLE_BOTTOM;
        }
        return BannerCloseStyle.STYLE_TOP;
    }
    // endregion

    // region MediationInterstitialAdapter implementation
    public static final String KEY_INTERSTITIAL_TYPE = "INTERSTITIAL_TYPE";
    private InterstitialAd mInterstitial;

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

        if (!DuAdMediation.checkClassExist("com.duapps.ad.InterstitialAd")) {
            String message = "The version of the Du Ad SDK included in this app does not include support for " +
                    "Interstitial ads. Please make sure that you are using the latest version of SDK";
            Log.e(TAG, message);
            Log.e(TAG, message);
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        int pid = getValidPid(serverParameters);
        String appId = serverParameters.getString(DuAdMediation.KEY_APP_ID);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        DuAdMediation.initializeSDK(context, mediationExtras, pid, appId);
        DuAdMediation.d(TAG, "requestInterstitialAd " + ",pid = " + pid + ",omInterstitial" + mInterstitial);
        DuAdExtrasBundleBuilder.InterstitialAdType type = null;
        if (mediationExtras != null) {
            type = (DuAdExtrasBundleBuilder.InterstitialAdType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
        }

        mInterstitial = new InterstitialAd(context, pid, getType(type));
        mInterstitial.setInterstitialListener(new DapCustomInterstitialEventForwarder(DuAdAdapter.this, listener));
        mInterstitial.load();
    }

    private InterstitialAd.Type getType(DuAdExtrasBundleBuilder.InterstitialAdType type) {
        if (type == null) {
            return InterstitialAd.Type.SCREEN;
        }
        if (type == DuAdExtrasBundleBuilder.InterstitialAdType.NORMAL) {
            return InterstitialAd.Type.NORMAL;
        }
        return InterstitialAd.Type.SCREEN;
    }

    @Override
    public void showInterstitial() {
        if (!DuAdMediation.checkClassExist("com.duapps.ad.InterstitialAd")) {
            String message = "The version of the Du Ad SDK included in this app does not include support for " +
                    "Interstitial ads. Please make sure that you are using the latest version of SDK";
            Log.e(TAG, message);
            Log.e(TAG, message);
            return;
        }
        if (mInterstitial != null) {
            DuAdMediation.d(TAG, "showInterstitial");
            mInterstitial.show();
        }
    }
    // endregion

    // region MediationAdapter implementation
    @Override
    public void onDestroy() {
        DuAdMediation.d(TAG, "onDestroy");
        if (mBannerAdView != null) {
            mBannerAdView.onDestory();
            mBannerAdView = null;
        }
        if (mInterstitial != null) {
            mInterstitial.destory();
            mInterstitial = null;
        }
        DuAdMediation.removeAllCallbacks();
    }

    @Override
    public void onPause() {
        DuAdMediation.d(TAG, "DuAdAdapter onPause");
    }

    @Override
    public void onResume() {
        DuAdMediation.d(TAG, "DuAdAdapter onResume");
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
