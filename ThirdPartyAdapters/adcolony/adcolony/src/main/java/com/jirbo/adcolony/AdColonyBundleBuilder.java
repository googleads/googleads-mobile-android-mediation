package com.jirbo.adcolony;

import android.os.Bundle;

public class AdColonyBundleBuilder {
    private static String _zoneId;
    private static boolean _showPreAdPopup;
    private static boolean _showPostAdPopup;
    private static String _userId;

    static public void setZoneId(String requestedZone) {
        _zoneId = requestedZone;
    }

    static public void setUserId(String userIdValue) {
        _userId = userIdValue;
    }

    static public void setShowPrePopup(boolean showPrePopupValue) {
        _showPreAdPopup = showPrePopupValue;
    }

    static public void setShowPostPopup(boolean showPostPopupValue) {
        _showPostAdPopup = showPostPopupValue;
    }

    static public Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putString("zone_id", _zoneId);
        bundle.putString("user_id", _userId);
        bundle.putBoolean("show_pre_popup", _showPreAdPopup);
        bundle.putBoolean("show_post_popup", _showPostAdPopup);
        return bundle;
    }
}
