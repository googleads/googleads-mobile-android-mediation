package com.google.ads.mediation.dap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.base.DuAdNetwork;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by bushaopeng on 18/2/2.
 */

public class DuAdMediation {
    private static final String TAG = DuAdMediation.class.getSimpleName();
    /**
     * This key should be configured at AdMob server side or AdMob front-end.
     */
    public static final String KEY_DAP_PID = "placementId";
    public static final String KEY_APP_ID = "appId";
    public static final String KEY_APP_LICENSE = "app_license";

    private static boolean DEBUG = false;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            String name = Thread.currentThread().getName();
            Log.d(TAG, "[" + tag + "]: " + "<" + name + "> " + msg);
        }
    }

    private static boolean isInitialized = false;
    public static final String KEY_ALL_PLACEMENT_ID = "ALL_PID";
    public static final String KEY_ALL_VIDEO_PLACEMENT_ID = "ALL_V_PID";
    private static HashSet<Integer> initializedPlacementIds = new HashSet<>();
    private static Handler handler;

    public static void runOnUIThread(Runnable runnable) {
        try {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(runnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeAllCallbacks() {
        if (handler != null) {
            try {
                handler.removeCallbacksAndMessages(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void initializeSDK(Context context, Bundle mediationExtras, int pid, String appId) {
        if (!isInitialized) {
            boolean initIdsSucc = false;
            boolean shouldInit = false;
            if (mediationExtras != null) {
                ArrayList<Integer> allPids = mediationExtras.getIntegerArrayList(KEY_ALL_PLACEMENT_ID);
                if (allPids != null) {
                    initializedPlacementIds.addAll(allPids);
                    shouldInit = true;
                    initIdsSucc = true;
                }
            }
            if (!initializedPlacementIds.contains(pid)) {
                initializedPlacementIds.add(pid);
                shouldInit = true;
            }
            if (shouldInit) {
                String initJsonConfig = buildJsonFromPidsNative(initializedPlacementIds, "native");
                d(TAG, "init config json is : " + initJsonConfig);
                setAppIdInMeta(context, appId);
                DuAdNetwork.init(context.getApplicationContext(), initJsonConfig);
                if (initIdsSucc) {
                    isInitialized = true;
                } else {
                    String msg = "Only the following placementIds " + initializedPlacementIds + " is initialized. "
                            + "It is Strongly recommended to use DuAdExtrasBundleBuilder.addAllPlacementId() to pass all "
                            + "your valid placement id (for native ad /banner ad/ interstitial ad) when requests ad, "
                            + "so that the DuAdNetwork could be normally initialized.";
                    Log.e(TAG, msg);
                }
            }
        }
    }

    static boolean setAppIdInMeta(Context context, String appId) {
        boolean succ = false;
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context
                    .getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                String appLicense = metaData.getString(KEY_APP_LICENSE);
                if (!TextUtils.isEmpty(appLicense)) {
                    DuAdMediation.d(TAG, "appId from meta is " + appLicense);
                    succ = true;
                }
                if (!TextUtils.isEmpty(appId)) {
                    DuAdMediation.d(TAG, "appId from admob server is " + appId);
                    metaData.putString(KEY_APP_LICENSE, appId);
                    succ = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return succ;
    }

    static String buildJsonFromPidsNative(@NonNull Collection<Integer> allPids, String node) {
        try {
            JSONStringer array = new JSONStringer().object().key(node).array();
            for (Integer pid : allPids) {
                array.object().key("pid").value(pid).endObject();
            }
            array.endArray().endObject();
            return array.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
