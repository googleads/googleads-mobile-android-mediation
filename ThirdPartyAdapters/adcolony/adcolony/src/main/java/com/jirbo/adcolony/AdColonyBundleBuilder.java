package com.jirbo.adcolony;

import android.os.Bundle;

/**
 * This is a helper class that helps publishers in creating a AdColony network-specific parameters
 * that can be used by the adapter to customize requests.
 */
public class AdColonyBundleBuilder {
    private static String _zoneId;
    private static boolean _showPreAdPopup;
    private static boolean _showPostAdPopup;
    private static String _userId;

    public static void setZoneId(String requestedZone) {
        _zoneId = requestedZone;
    }

    public static void setUserId(String userIdValue) {
        _userId = userIdValue;
    }

    public static void setShowPrePopup(boolean showPrePopupValue) {
        _showPreAdPopup = showPrePopupValue;
    }

    public static void setShowPostPopup(boolean showPostPopupValue) {
        _showPostAdPopup = showPostPopupValue;
    }

    public static Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putString("zone_id", _zoneId);
        bundle.putString("user_id", _userId);
        bundle.putBoolean("show_pre_popup", _showPreAdPopup);
        bundle.putBoolean("show_post_popup", _showPostAdPopup);
        return bundle;
    }
}
