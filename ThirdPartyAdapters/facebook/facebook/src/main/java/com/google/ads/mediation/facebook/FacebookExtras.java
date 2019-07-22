
package com.google.ads.mediation.facebook;

import android.os.Bundle;

public class FacebookExtras {
    public static String NATIVE_BANNER = "native_banner";
    private static boolean _nativeBanner;

    public FacebookExtras setNativeBanner(boolean nativeBanner) {
        _nativeBanner = nativeBanner;
        return this;
    }

    public Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(NATIVE_BANNER, _nativeBanner);
        return bundle;
    }
}
