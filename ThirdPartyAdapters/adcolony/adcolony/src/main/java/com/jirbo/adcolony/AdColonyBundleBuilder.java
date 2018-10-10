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
    private static String _gdprConsentString;
    private static boolean _gdprRequired;

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

    /**
     * This is to inform the AdColony service if GDPR should be considered for the user based on
     * if they are EU citizens or from EU territories.
     *
     * @param gdprRequired whether or not we need to consider GDPR for this user.
     */
    public static void setGdprRequired(boolean gdprRequired) {
        _gdprRequired = gdprRequired;
    }

    /**
     * Used to set the user's GDPR consent String. This was originally designed for IAB compliance,
     * but for now please follow AdColony's GDPR documentation for setting this value:
     * https://github.com/AdColony/AdColony-Android-SDK-3/wiki/GDPR
     *
     * @param gdprConsentString the user's GDPR consent String
     */
    public static void setGdprConsentString(String gdprConsentString) {
        _gdprConsentString = gdprConsentString;
    }

    public static Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putString("zone_id", _zoneId);
        bundle.putString("user_id", _userId);
        bundle.putBoolean("show_pre_popup", _showPreAdPopup);
        bundle.putBoolean("show_post_popup", _showPostAdPopup);
        bundle.putBoolean("gdpr_required", _gdprRequired);
        bundle.putString("gdpr_consent_string", _gdprConsentString);
        return bundle;
    }
}
