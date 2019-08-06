package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdSize;

import java.util.ArrayList;

/**
 * A helper class for the myTarget adapter.
 */
class MyTargetTools {
    private static final String KEY_SLOT_ID = "slotId";
    @NonNull
    static final String PARAM_MEDIATION_KEY = "mediation";
    @NonNull
    static final String PARAM_MEDIATION_VALUE = "1";

    /**
     * Checks params taken from Google. MyTarget slotId must be only positive, so if we return
     * negative number, it was invalid request.
     *
     * @param context          app context.
     * @param serverParameters bundle with server params, must contain myTarget slot ID.
     * @return myTarget slot ID, or negative number, if something gone wrong and we should return
     * invalid request callback to Google SDK.
     */
    static int checkAndGetSlotId(final @Nullable Context context,
                                 final @Nullable Bundle serverParameters) {
        int slotId = -1;

        if (context == null) {
            Log.w(MyTargetMediationAdapter.TAG,
                    "Failed to request ad from MyTarget: Context is null.");
            return slotId;
        }

        if (serverParameters == null) {
            Log.w(MyTargetMediationAdapter.TAG,
                    "Failed to request ad from MyTarget: serverParameters is null.");
        } else {
            String slotIdParam = serverParameters.getString(KEY_SLOT_ID);
            if (TextUtils.isEmpty(slotIdParam)) {
                Log.w(MyTargetMediationAdapter.TAG,
                        "Failed to request ad from MyTarget: Missing or Invalid Slot ID.");
            } else {
                try {
                    slotId = Integer.parseInt(slotIdParam);
                } catch (NumberFormatException ex) {
                    Log.w(MyTargetMediationAdapter.TAG, "Failed to request ad from MyTarget.", ex);
                }
            }
        }
        return slotId;
    }

  // Start of helper code to remove when available in SDK
  /**
   * Find the closest supported AdSize from the list of potentials to the provided size.
   * Returns null if none are within given threshold size range.
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
