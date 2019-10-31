package com.google.ads.mediation.adcolony;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.adcolony.sdk.AdColonyAdSize;
import com.google.android.gms.ads.AdSize;

import java.util.ArrayList;

public class AdColonyAdapterUtils {

    public static final String KEY_APP_ID = "app_id";
    public static final String KEY_ZONE_ID = "zone_id";
    public static final String KEY_ZONE_IDS = "zone_ids";

    public static AdColonyAdSize adColonyAdSizeFromAdMobAdSize(Context context, AdSize adSize) {
        ArrayList<AdSize> potentials = new ArrayList<>(3);
        potentials.add(0, AdSize.BANNER);
        potentials.add(1, AdSize.LEADERBOARD);
        potentials.add(2, AdSize.MEDIUM_RECTANGLE);
        potentials.add(3, AdSize.WIDE_SKYSCRAPER);

        AdSize closestSize = AdColonyAdapterUtils.findClosestSize(context, adSize, potentials);

        if (AdSize.BANNER.equals(closestSize)) {
            return AdColonyAdSize.BANNER;
        } else if (AdSize.MEDIUM_RECTANGLE.equals(closestSize)) {
            return AdColonyAdSize.MEDIUM_RECTANGLE;
        } else if (AdSize.LEADERBOARD.equals(closestSize)) {
            return AdColonyAdSize.LEADERBOARD;
        } else if (AdSize.WIDE_SKYSCRAPER.equals(closestSize)) {
            return AdColonyAdSize.SKYSCRAPER;
        } else {
            return null;
        }
    }

    /**
     * Find the closest supported AdSize from the list of potentials to the provided size.
     * Returns null if none are within given threshold size range.
     */
    private static AdSize findClosestSize(Context context,
                                         AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
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

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into dp
     * @return A int value to represent dp equivalent to px value
     */
    public static int convertPixelsToDp(int px){
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

}
