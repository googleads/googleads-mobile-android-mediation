package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.text.TextUtils;
import android.view.View;

import com.duapps.ad.banner.BannerAdView;
import com.google.ads.mediation.dap.forwarder.DapCustomBannerEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;

import java.io.Serializable;

/**
 * Created by bushaopeng on 18/1/3.
 */
@Keep
public class DuBannerAdAdapter implements MediationBannerAdapter {
    private static final String TAG = DuBannerAdAdapter.class.getSimpleName();

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
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        DuAdAdapter.d(TAG, "requestBannerAd" + ",pid = " + pid);
        mBannerAdView = new BannerAdView(context, pid, 5,
                new DapCustomBannerEventForwarder(DuBannerAdAdapter.this, listener));
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



    /* ****************** MediationAdapter ********************** */

    @Override
    public void onDestroy() {
        DuAdAdapter.d(TAG, "onDestroy ");
        if (mBannerAdView != null) {
            mBannerAdView.onDestory();
            mBannerAdView = null;
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


        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_BANNER_CLOSE_STYLE, bannerCloseStyle);
            bundle.putSerializable(KEY_BANNER_STYLE, bannerStyle);
            return bundle;
        }
    }
}
