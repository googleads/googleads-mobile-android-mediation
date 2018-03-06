package com.google.ads.mediation.ironsource;

import android.app.Activity;
import com.ironsource.mediationsdk.IronSource;

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
