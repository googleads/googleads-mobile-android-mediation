package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.ironsource.mediationsdk.IronSource;

/**
 * Created by gili.ariel on 20/12/2017.
 */

class IronSourceBaseAdapter {

    /**
     * Adapter class name for logging
     */
    static final String TAG = "IronSource";

    /**
     * Key to obtain App Key, required for initializing IronSource SDK.
     */
    static final String KEY_APP_KEY = "appKey";

    /**
     * Constant used for IronSource internal reporting
     */
    static final String MEDIATION_NAME = "AdMob";

    /**
     * Key to obtain isTestEnabled flag, used to control console logs display
     */
    static final String KEY_TEST_MODE = "isTestEnabled";

    /**
     * This is used for show logs inside the adapter
     */
    boolean mIsTestEnabled;

    /**
     * This is used to indicate if we initiated IronSource's SDK inside the adapter
     */
    private boolean mInitSucceeded;

    /**
     * UI thread handler used to send callbacks with AdMob interface
     */
    private Handler mUIHandler;

    void initIronSourceSDK(Context context, Bundle serverParameters, IronSource.AD_UNIT adUnit) {
        // 1 - We are not sending user ID from adapters anymore,
        //     the IronSource SDK will take care of this identifier

        // 2 - We assume the init is always successful (we will fail in load if needed)

        // SDK requires activity context to initialize, so check that the context
        // provided by the app is an activity context before initializing

        if (!mInitSucceeded) {
            mIsTestEnabled = serverParameters.getBoolean(KEY_TEST_MODE, false);

            String appKey = serverParameters.getString(KEY_APP_KEY);

            IronSource.setMediationType(MEDIATION_NAME);
            IronSource.init((Activity) context, appKey, adUnit);

            mInitSucceeded = true;
        }
    }

    synchronized void sendEventOnUIThread(Runnable runnable) {
        if (mUIHandler == null) {
            mUIHandler = new Handler(Looper.getMainLooper());
        }
        mUIHandler.post(runnable);
    }

    void onLog(String message) {
        if (mIsTestEnabled) {
            Log.d(TAG, message);
        }
    }
}
