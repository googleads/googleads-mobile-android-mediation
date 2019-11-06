package com.google.ads.mediation.imobile;

import com.google.android.gms.ads.AdRequest;

import jp.co.imobile.sdkads.android.FailNotificationReason;

/**
 * Helper of mediation adapter.
 */
public final class AdapterHelper {

    /**
     * Convert i-mobile fail reason to AdMob error code.
     *
     * @param reason i-mobile fail reason
     * @return AdMob error code
     */
    public static int convertToAdMobErrorCode(FailNotificationReason reason) {
        // Convert i-mobile fail reason to AdMob error code.
        switch (reason) {
            case RESPONSE:
            case UNKNOWN:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
            case PARAM:
            case AUTHORITY:
            case PERMISSION:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case NETWORK_NOT_READY:
            case NETWORK:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case AD_NOT_READY:
            case NOT_DELIVERY_AD:
            case SHOW_TIMEOUT:
                return AdRequest.ERROR_CODE_NO_FILL;
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}
