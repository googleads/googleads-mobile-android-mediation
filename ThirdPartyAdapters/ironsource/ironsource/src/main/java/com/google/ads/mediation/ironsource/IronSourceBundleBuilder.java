package com.google.ads.mediation.ironsource;


import android.os.Bundle;

import static com.google.ads.mediation.ironsource.IronSourceBaseAdapter.KEY_TEST_MODE;


/**
 * This is a helper class that helps publishers in creating a IronSource network-specific parameters
 * that can be used by the adapter to customize requests.
 */
public class IronSourceBundleBuilder {

    private static boolean mDebugEnabled = false;

    public static void setDebug(boolean debug) {
        mDebugEnabled = debug;
    }

    public static Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_TEST_MODE, mDebugEnabled);
        return bundle;
    }
}