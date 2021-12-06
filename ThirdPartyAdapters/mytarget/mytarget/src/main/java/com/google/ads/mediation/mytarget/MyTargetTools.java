package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdSize;
import com.my.target.ads.MyTargetView;
import com.my.target.common.CustomParams;

/**
 * A helper class for the myTarget adapter.
 */
class MyTargetTools {

  private static final String KEY_SLOT_ID = "slotId";
  @NonNull
  static final String PARAM_MEDIATION_KEY = "mediation";
  @NonNull
  static final String PARAM_MEDIATION_VALUE = "1";

  public static final int MIN_BANNER_HEIGHT_DP = 50;
  public static final float MIN_BANNER_PROPORTION = 0.75f;

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

  @Nullable
  static MyTargetView.AdSize getSupportedAdSize(@NonNull AdSize requestedSize,
      @NonNull Context context) {
    int width = requestedSize.getWidth();
    if (width < 0) {
      int widthInPixels = requestedSize.getWidthInPixels(context);
      width = MyTargetTools.toDips(widthInPixels, context);
    }

    int height = requestedSize.getHeight();
    if (height < 0) {
      int heightInPixels = requestedSize.getHeightInPixels(context);
      height = MyTargetTools.toDips(heightInPixels, context);
    }

    if (width == 300 && height == 250) {
      return MyTargetView.AdSize.ADSIZE_300x250;
    } else if (width == 728 && height == 90) {
      return MyTargetView.AdSize.ADSIZE_728x90;
    } else if (width == 320 && height == 50) {
      return MyTargetView.AdSize.ADSIZE_320x50;
    } else if (width > 0 && height >= MIN_BANNER_HEIGHT_DP
        && height < MIN_BANNER_PROPORTION * width) {
      return MyTargetView.AdSize.getAdSizeForCurrentOrientation(width, height, context);
    }

    return null;
  }

  static int toDips(int pixels, @NonNull Context context) {
    return Math.round(pixels / (((float) context.getResources().getDisplayMetrics().densityDpi)
        / DisplayMetrics.DENSITY_DEFAULT));
  }

  public static void handleMediationExtras(@NonNull String tag, @Nullable Bundle mediationExtras, @NonNull CustomParams customParams) {
    if (mediationExtras == null) {
      Log.d(tag, "Mediation extras is null");

      return;
    }

    Log.d(tag, "Mediation extras size: " + mediationExtras.size());
    for (String key : mediationExtras.keySet()) {
      final Object object = mediationExtras.get(key);
      if (object == null) {
        customParams.setCustomParam(key, null);
        Log.d(tag, "Add null custom param from mediation extra: " + key + ", " + "null");
      } else if (object instanceof Boolean) {
        final String value = (boolean) object ? "1" : "0";
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add boolean custom param from mediation extra: " + key + ", " + value);
      } else if (object instanceof Byte) {
        final String value = String.valueOf((byte) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add byte custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Short) {
        final String value = String.valueOf((short) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add short custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Integer) {
        final String value = String.valueOf((int) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add integer custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Long) {
        final String value = String.valueOf((long) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add long custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Float) {
        final String value = String.valueOf((float) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add float custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Double) {
        final String value = String.valueOf((double) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add double custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof Character) {
        final String value = String.valueOf((char) object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add character custom param from mediation extra: " + key + ", " + object);
      } else if (object instanceof String) {
        final String value = String.valueOf(object);
        customParams.setCustomParam(key, value);
        Log.d(tag, "Add string custom param from mediation extra: " + key + ", " + object);
      }
      else {
        Log.d(tag, "Mediation extra has non-primitive extra that will not be added: " + key + ", " + object);
      }
    }
  }
}
