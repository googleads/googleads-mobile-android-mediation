package com.google.ads.mediation.dap;

import android.support.annotation.Keep;
import android.util.Log;

/**
 * Created by bushaopeng on 18/1/4.
 */
@Keep
public class DuAd {
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
}
