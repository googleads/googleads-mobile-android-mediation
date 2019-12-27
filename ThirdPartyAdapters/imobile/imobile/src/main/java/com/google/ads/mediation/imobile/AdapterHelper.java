package com.google.ads.mediation.imobile;

import android.content.Context;
import com.google.android.gms.ads.AdRequest;

import com.google.android.gms.ads.AdSize;
import java.util.ArrayList;
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

    // Start of helper code to remove when available in SDK
    /**
     * Find the closest supported AdSize from the list of potentials to the provided size.
     *
     * @param context the activity context
     * @param original the original requested ad size
     * @param potentials the supported ad sizes
     * @return null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
        Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context)/density);
        int actualHeight = Math.round(original.getHeightInPixels(context)/density);
        original = new AdSize(actualWidth, actualHeight);
        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    private static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth ||
            originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight ||
            originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK
}
