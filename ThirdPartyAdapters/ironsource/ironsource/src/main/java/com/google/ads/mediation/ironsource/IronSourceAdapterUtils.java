package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.ironsource.mediationsdk.IronSource;

/*
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK, as well as some helper methods for the IronSource adapters.
 */
public class IronSourceAdapterUtils {

    /**
     * Adapter class name for logging.
     */
    static final String TAG = IronSourceMediationAdapter.class.getSimpleName();

    /**
     * Key to obtain App Key, required for initializing IronSource SDK.
     */
    static final String KEY_APP_KEY = "appKey";

    /**
     * Constant used for IronSource internal reporting.
     */
    static final String MEDIATION_NAME = "AdMob";

    /**
     * Key to obtain the IronSource Instance ID, required to shot IronSource ads.
     */
    static final String KEY_INSTANCE_ID = "instanceId";

    /**
     * UI thread handler used to send callbacks with AdMob interface.
     */
    private static Handler uiHandler;

    private static boolean mIsIronSourceInitialized = false;

    public static void onActivityPaused(Activity activity)
    {
        IronSource.onPause(activity);
    }

    public static void onActivityResumed(Activity activity)
    {
        IronSource.onResume(activity);
    }

    static void initIronSourceSDK(Activity activity,
                                  String appKey,
                                  IronSource.AD_UNIT adUnit) {
        IronSource.setMediationType(MEDIATION_NAME);
        IronSource.initISDemandOnly(activity, appKey, adUnit);
        mIsIronSourceInitialized = true;
    }

    static boolean isIronSourceInitialized() {
        return mIsIronSourceInitialized;
    }

    static synchronized void sendEventOnUIThread(Runnable runnable) {
        if (uiHandler == null) {
            uiHandler = new Handler(Looper.getMainLooper());
        }
        uiHandler.post(runnable);
    }
}
