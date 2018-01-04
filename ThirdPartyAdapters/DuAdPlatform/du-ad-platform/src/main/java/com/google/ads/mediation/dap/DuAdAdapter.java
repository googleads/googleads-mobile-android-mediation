package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.duapps.ad.DuNativeAd;
import com.duapps.ad.InterstitialAd;
import com.duapps.ad.banner.BannerAdView;
import com.duapps.ad.banner.BannerListener;
import com.google.ads.mediation.dap.forwarder.DapCustomInterstitialEventForwarder;
import com.google.ads.mediation.dap.forwarder.DapCustomNativeEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;

import java.io.Serializable;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuAdAdapter implements MediationBannerAdapter, MediationInterstitialAdapter,
        MediationNativeAdapter {
    private static final String TAG = "DAP Mediation";
    /**
     * This key should be configured at AdMob server side or AdMob front-end.
     */
    public static final String KEY_DAP_PID = "placementId";
    public static final String KEY_BANNER_STYLE = "BANNER_STYLE";
    public static final String KEY_BANNER_CLOSE_STYLE = "BANNER_CLOSE_STYLE";
    public static final String KEY_INTERSTITIAL_TYPE = "INTERSTITIAL_TYPE";
    public static final String KEY_SOURCE = "source";
    private BannerAdView mBannerAdView;
    private InterstitialAd mInterstitial;
    private DuNativeAd nativeAd;

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
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        Log.d(TAG, "requestBannerAd" + ",pid = " + pid);
        mBannerAdView = new BannerAdView(context, pid, 5, new BannerListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, " onAdLoaded , mAdView = " + mBannerAdView);
                listener.onAdLoaded(DuAdAdapter.this);
            }

            @Override
            public void onError(String s) {
                Log.d(TAG, "onError - " + s);
                listener.onAdFailedToLoad(DuAdAdapter.this, Integer.valueOf(s));
            }
        });
        BannerStyle style = (BannerStyle) mediationExtras.getSerializable(KEY_BANNER_STYLE);
        BannerCloseStyle closeStyle = (BannerCloseStyle) mediationExtras.getSerializable(KEY_BANNER_CLOSE_STYLE);
        mBannerAdView.setBgStyle(getStyle(style));
        mBannerAdView.setCloseStyle(getCloseStyle(closeStyle));

        mBannerAdView.load();
    }

    @Override
    public View getBannerView() {
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
        Log.d(TAG, "requestInterstitialAd " + ",pid = " + pid + ",omInterstitial" + mInterstitial);
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
        if (mInterstitial != null) {
            Log.d(TAG, "showInterstitial ");
            mInterstitial.show();
        }
    }

    /* ****************** MediationNativeAdapter ********************** */

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
        nativeAd = new DuNativeAd(context, pid);
        nativeAd.setMobulaAdListener(new DapCustomNativeEventForwarder(DuAdAdapter.this, listener));

        nativeAd.load();
    }

    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy ");

        if (nativeAd != null) {
            nativeAd.destory();
            nativeAd = null;
        }
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
        Log.d(TAG, "DuAdAdapter onPause");
    }

    @Override
    public void onResume() {
        Log.d(TAG, "DuAdAdapter onResume");
    }


    private int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(KEY_DAP_PID);
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

    public enum BannerCloseStyle implements Serializable {
        STYLE_TOP,
        STYLE_BOTTOM;
    }

    public enum BannerStyle implements Serializable {
        STYLE_BLUE,
        STYLE_GREEN;
    }

    public static class DuAdExtrasBundleBuilder {

        /**
         * only used for banner ad, choose a banner close style
         */
        private BannerCloseStyle bannerCloseStyle;
        /**
         * only used for banner ad, choose a banner style
         */
        private BannerStyle bannerStyle;
        /**
         * only used for interstitial Ad, choose a banner style
         */
        private InterstitialAdType interstitialAdType;

        public DuAdExtrasBundleBuilder bannerCloseStyle(BannerCloseStyle bannerCloseStyle) {
            this.bannerCloseStyle = bannerCloseStyle;
            return DuAdExtrasBundleBuilder.this;
        }

        public DuAdExtrasBundleBuilder bannerStyle(BannerStyle bannerStyle) {
            this.bannerStyle = bannerStyle;
            return DuAdExtrasBundleBuilder.this;
        }

        public DuAdExtrasBundleBuilder interstitialAdType(InterstitialAdType interstitialAdType) {
            this.interstitialAdType = interstitialAdType;
            return DuAdExtrasBundleBuilder.this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_BANNER_CLOSE_STYLE, bannerCloseStyle);
            bundle.putSerializable(KEY_BANNER_STYLE, bannerStyle);
            bundle.putSerializable(KEY_INTERSTITIAL_TYPE, interstitialAdType);
            return bundle;
        }
    }
}
