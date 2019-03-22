package com.google.ads.mediation.ironsource;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;

import java.util.HashSet;

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
     * Key to obtain the IronSource Instance ID, required to show IronSource ads.
     */
    static final String KEY_INSTANCE_ID = "instanceId";

    /**
     * Constant used for IronSource internal reporting.
     */
    private static final String MEDIATION_NAME = "AdMob";

    /**
     * UI thread handler used to send callbacks with AdMob interface.
     */
    private static Handler uiHandler;

    /**
     * Set of {@link com.ironsource.mediationsdk.IronSource.AD_UNIT} that have been initialized.
     */
    private static HashSet<IronSource.AD_UNIT> mInitialized = new HashSet<>();

    static void initIronSourceSDK(Activity activity,
                                  String appKey,
                                  IronSource.AD_UNIT adUnit) {
        if (isIronSourceInitialized(adUnit)) {
            Log.d(IronSourceAdapterUtils.TAG,
                    adUnit.toString() + " has already been initialized.");
        } else {
            IronSource.setMediationType(MEDIATION_NAME);
            IronSource.initISDemandOnly(activity, appKey, adUnit);
            mInitialized.add(adUnit);
        }
    }

    static boolean isIronSourceInitialized(IronSource.AD_UNIT ad_unit) {
        return mInitialized.contains(ad_unit);
    }

    static synchronized void sendEventOnUIThread(Runnable runnable) {
        if (uiHandler == null) {
            uiHandler = new Handler(Looper.getMainLooper());
        }
        uiHandler.post(runnable);
    }
}