package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;

/*
 * A helper class used by {@link IronSourceAdapter} and {@link IronSourceRewardedAdapter}.
 */
class IronSourceBaseAdapter {

    /**
     * Adapter class name for logging.
     */
    static final String TAG = "IronSource";

    /**
     * Key to obtain App Key, required for initializing IronSource SDK.
     */
    static final String KEY_APP_KEY = "appKey";

    /**
     * Constant used for IronSource internal reporting.
     */
    static final String MEDIATION_NAME = "AdMob";

    /**
     * Key to obtain isTestEnabled flag, used to control console logs display.
     */
    static final String KEY_INSTANCE_ID = "instanceId";

    /**
     * This is used for show logs inside the adapter.
     */
     public boolean mIsLogEnabled;

    /**
     * This is the id of the instance to be shown.
     */
    public String mInstanceID;

    /**
     * UI thread handler used to send callbacks with AdMob interface.
     */
    private Handler mUIHandler;

    void initIronSourceSDK(Context context, String appKey, IronSource.AD_UNIT adUnit) {
        IronSource.setMediationType(MEDIATION_NAME);
        IronSource.initISDemandOnly((Activity) context, appKey, adUnit);
    }

    synchronized void sendEventOnUIThread(Runnable runnable) {
        if (mUIHandler == null) {
            mUIHandler = new Handler(Looper.getMainLooper());
        }
        mUIHandler.post(runnable);
    }

    void onLog(String message) {
        if (mIsLogEnabled) {
            Log.d(TAG, message);
        }
    }
}
