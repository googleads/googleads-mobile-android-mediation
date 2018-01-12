package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.duapps.ad.InterstitialAd;
import com.duapps.ad.banner.BannerAdView;
import com.google.ads.mediation.dap.forwarder.DapCustomBannerEventForwarder;
import com.google.ads.mediation.dap.forwarder.DapCustomInterstitialEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import java.io.Serializable;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuAdAdapter implements MediationBannerAdapter, MediationInterstitialAdapter {
    private static final String TAG = DuAdAdapter.class.getSimpleName();
    /**
     * This key should be configured at AdMob server side or AdMob front-end.
     */
    public static final String KEY_DAP_PID = "placementId";
    private static boolean DEBUG = false;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static final String KEY_BANNER_STYLE = "BANNER_STYLE";
    public static final String KEY_BANNER_CLOSE_STYLE = "BANNER_CLOSE_STYLE";
    private BannerAdView mBannerAdView;

    /* ****************** MediationBannerAdapter ********************** */

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
        if (!checkClassExist("com.duapps.ad.banner.BannerAdView")) {
            String msg = "Your version of Du Ad SDK is not right, there is no support for Banner Ad in current SDK, "
                    + "Please make sure that you are using CW latest version of SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        DuAdAdapter.d(TAG, "requestBannerAd" + ",pid = " + pid);
        mBannerAdView = new BannerAdView(context, pid, 5,
                new DapCustomBannerEventForwarder(DuAdAdapter.this, listener));
        BannerStyle style = (BannerStyle) mediationExtras.getSerializable(KEY_BANNER_STYLE);
        BannerCloseStyle closeStyle = (BannerCloseStyle) mediationExtras.getSerializable(KEY_BANNER_CLOSE_STYLE);
        mBannerAdView.setBgStyle(getStyle(style));
        mBannerAdView.setCloseStyle(getCloseStyle(closeStyle));

        mBannerAdView.load();
    }

    @Override
    public View getBannerView() {
        if (!checkClassExist("com.duapps.ad.banner.BannerAdView")) {
            String msg = "Your version of Du Ad SDK is not right, there is no support for Banner Ad in current SDK, "
                    + "Please make sure that you are using CW latest version of SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            return null;
        }
        return mBannerAdView;
    }

    private com.duapps.ad.banner.BannerStyle getStyle(BannerStyle style) {
        if (style == null) {
            return com.duapps.ad.banner.BannerStyle.STYLE_GREEN;
        }
        if (style == BannerStyle.STYLE_BLUE) {
            return com.duapps.ad.banner.BannerStyle.STYLE_BLUE;
        }
        return com.duapps.ad.banner.BannerStyle.STYLE_GREEN;
    }

    private com.duapps.ad.banner.BannerCloseStyle getCloseStyle(BannerCloseStyle style) {
        if (style == null) {
            return com.duapps.ad.banner.BannerCloseStyle.STYLE_TOP;
        }
        if (style == BannerCloseStyle.STYLE_BOTTOM) {
            return com.duapps.ad.banner.BannerCloseStyle.STYLE_BOTTOM;
        }
        return com.duapps.ad.banner.BannerCloseStyle.STYLE_TOP;
    }


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
        if (!checkClassExist("com.duapps.ad.InterstitialAd")) {
            String msg = "Your version of Du Ad SDK is not right, there is no support for Interstitial Ad in current "
                    + "SDK, Please make sure that you are using CW latest version of SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
        mInterstitial.setInterstitialListener(new DapCustomInterstitialEventForwarder(DuAdAdapter.this, listener));
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
        if (!checkClassExist("com.duapps.ad.InterstitialAd")) {
            String msg = "Your version of Du Ad SDK is not right, there is no support for Interstitial Ad in current "
                    + "SDK, Please make sure that you are using CW latest version of SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            return;
        }
        if (mInterstitial != null) {
            DuAdAdapter.d(TAG, "showInterstitial ");
            mInterstitial.show();
        }
    }
    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        DuAdAdapter.d(TAG, "onDestroy ");
        if (mBannerAdView != null) {
            mBannerAdView.onDestory();
            mBannerAdView = null;
        }
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

    public enum BannerCloseStyle implements Serializable {
        STYLE_TOP,
        STYLE_BOTTOM;
    }

    public enum BannerStyle implements Serializable {
        STYLE_BLUE,
        STYLE_GREEN;
    }

    public static class ExtrasBundleBuilder {

        private BannerCloseStyle bannerCloseStyle;
        private BannerStyle bannerStyle;

        public ExtrasBundleBuilder bannerCloseStyle(BannerCloseStyle bannerCloseStyle) {
            this.bannerCloseStyle = bannerCloseStyle;
            return ExtrasBundleBuilder.this;
        }

        public ExtrasBundleBuilder bannerStyle(BannerStyle bannerStyle) {
            this.bannerStyle = bannerStyle;
            return ExtrasBundleBuilder.this;
        }

        private InterstitialAdType interstitialAdType;

        public ExtrasBundleBuilder interstitialAdType(InterstitialAdType interstitialAdType) {
            this.interstitialAdType = interstitialAdType;
            return ExtrasBundleBuilder.this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_BANNER_CLOSE_STYLE, bannerCloseStyle);
            bundle.putSerializable(KEY_BANNER_STYLE, bannerStyle);
            bundle.putSerializable(KEY_INTERSTITIAL_TYPE, interstitialAdType);
            return bundle;
        }
    }

    public enum InterstitialAdType implements Serializable {
        NORMAL,
        SCREEN;
    }


    public static boolean checkClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
