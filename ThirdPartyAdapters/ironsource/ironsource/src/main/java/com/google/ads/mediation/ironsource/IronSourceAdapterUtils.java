package com.google.ads.mediation.ironsource;

import android.app.Activity;
import com.ironsource.mediationsdk.IronSource;

/*
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK.
 */
public class IronSourceAdapterUtils {

    public static void onActivityPaused(Activity activity)
    {
        IronSource.onPause(activity);
    }

    public static void onActivityResumed(Activity activity)
    {
        IronSource.onResume(activity);
    }
}
