package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
   * @param context app context.
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

}
