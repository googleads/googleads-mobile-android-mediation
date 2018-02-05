package com.google.ads.mediation.dap;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(runnable);
    }

    public static void removeAllCallbacks() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    static void initializeSDK(Context context, Bundle mediationExtras, int pid) {
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
//                try {
//                    ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context
//                            .getPackageName(), ApplicationInfo.EXT);
//                } catch (PackageManager.NameNotFoundException e) {
//                    e.printStackTrace();
//                }
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
