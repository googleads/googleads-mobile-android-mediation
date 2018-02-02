package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.duapps.ad.InterstitialAd;
import com.duapps.ad.banner.BannerAdView;
import com.duapps.ad.base.DuAdNetwork;
import com.google.ads.mediation.dap.forwarder.DapCustomBannerEventForwarder;
import com.google.ads.mediation.dap.forwarder.DapCustomInterstitialEventForwarder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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
    public static final String KEY_ALL_PLACEMENT_ID = "ALL_PID";
    public static final String KEY_ALL_VIDEO_PLACEMENT_ID = "ALL_V_PID";
    private static boolean DEBUG = false;
    static boolean isInitialized = false;

    public static HashSet<Integer> initializedPlacementIds = new HashSet<>();

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
        int pid = getValidPid(serverParameters);
        if (pid < 0) {
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        initializeSDK(context, mediationExtras, pid);
        if (!checkClassExist("com.duapps.ad.banner.BannerAdView")) {
            String msg = "Your version of Du Ad SDK is not right, there is no support for Banner Ad in current SDK, "
                    + "Please make sure that you are using CW latest version of SDK";
            Log.e(TAG, msg);
            Log.e(TAG, msg);
            listener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            return;
        }

        DuAdAdapter.d(TAG, "requestBannerAd" + ",pid = " + pid);
        mBannerAdView = new BannerAdView(context, pid, 5,
                new DapCustomBannerEventForwarder(DuAdAdapter.this, listener));
        DuAdExtrasBundleBuilder.BannerStyle style = (DuAdExtrasBundleBuilder.BannerStyle) mediationExtras
                .getSerializable(KEY_BANNER_STYLE);
        DuAdExtrasBundleBuilder.BannerCloseStyle closeStyle = (DuAdExtrasBundleBuilder.BannerCloseStyle)
                mediationExtras.getSerializable
                        (KEY_BANNER_CLOSE_STYLE);
        mBannerAdView.setBgStyle(getStyle(style));
        mBannerAdView.setCloseStyle(getCloseStyle(closeStyle));

        mBannerAdView.load();
    }

    static void initializeSDK(Context context, Bundle mediationExtras, int pid) {
        if (!isInitialized) {
            ArrayList<Integer> allPids = mediationExtras.getIntegerArrayList(KEY_ALL_PLACEMENT_ID);
            if (allPids != null) {
                if (!allPids.contains(pid)) {
                    allPids.add(pid);
                }
                initializedPlacementIds.addAll(allPids);
                String initJsonConfig = buildJsonFromPidsNative(initializedPlacementIds);
                d(TAG, "init config json is : " + initJsonConfig);
                DuAdNetwork.init(context, initJsonConfig);
                isInitialized = true;
            } else {
                if (!initializedPlacementIds.contains(pid)) {
                    initializedPlacementIds.add(pid);
                    String initJsonConfig = buildJsonFromPidsNative(initializedPlacementIds);
                    d(TAG, "init config json is : " + initJsonConfig);
                    DuAdNetwork.init(context, initJsonConfig);
                    String msg = "Only the current placementIds " + initializedPlacementIds + " is initialized. "
                            + "It is Strongly recommended to use DuAdExtrasBundleBuilder.addAllPlacementId() to pass all "
                            + "your valid placement id (for native ad /banner ad/ interstitial ad), "
                            + "so that the DuAdNetwork could be normally initialized.";
                    Log.e(TAG, msg);
                }
            }
        }
    }

    static String buildJsonFromPidsNative(@NonNull Collection<Integer> allPids) {
        try {
            JSONStringer array = new JSONStringer().object().key("native").array();
            for (Integer pid : allPids) {
                array.object().key("pid").value(pid);
            }
            return array.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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

    private com.duapps.ad.banner.BannerStyle getStyle(DuAdExtrasBundleBuilder.BannerStyle style) {
        if (style == null) {
            return com.duapps.ad.banner.BannerStyle.STYLE_GREEN;
        }
        if (style == DuAdExtrasBundleBuilder.BannerStyle.STYLE_BLUE) {
            return com.duapps.ad.banner.BannerStyle.STYLE_BLUE;
        }
        return com.duapps.ad.banner.BannerStyle.STYLE_GREEN;
    }

    private com.duapps.ad.banner.BannerCloseStyle getCloseStyle(DuAdExtrasBundleBuilder.BannerCloseStyle style) {
        if (style == null) {
            return com.duapps.ad.banner.BannerCloseStyle.STYLE_TOP;
        }
        if (style == DuAdExtrasBundleBuilder.BannerCloseStyle.STYLE_BOTTOM) {
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
        initializeSDK(context, mediationExtras, pid);
        DuAdAdapter.d(TAG, "requestInterstitialAd " + ",pid = " + pid + ",omInterstitial" + mInterstitial);
        DuAdExtrasBundleBuilder.InterstitialAdType type = (DuAdExtrasBundleBuilder.InterstitialAdType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
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


    public static boolean checkClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
