package com.google.ads.mediation.verizon;

import android.os.Handler;
import android.os.Looper;


/**
 * Utility class for managing task execution in a safe way on the UI Thread
 */
class ThreadUtils {

    private static Handler uiHandler;


    static void postOnUiThread(final Runnable runnable) {

        if (uiHandler == null) {
            uiHandler = new Handler(Looper.getMainLooper());
        }
        uiHandler.post(runnable);
    }
}
