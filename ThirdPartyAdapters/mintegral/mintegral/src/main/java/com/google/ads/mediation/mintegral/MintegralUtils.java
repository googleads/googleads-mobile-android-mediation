package com.google.ads.mediation.mintegral;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;

public class MintegralUtils {

  public static final String TAG = MintegralUtils.class.getSimpleName();

  /**
   * Determines whether the ad should be muted based on the provided network extras.
   */
  public static boolean shouldMuteAudio(@NonNull Bundle networkExtras) {
    return networkExtras != null && networkExtras.getBoolean(MintegralExtras.Keys.MUTE_AUDIO);
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
          @Nullable String adUnitId,
          @Nullable String placementId) {
    if (TextUtils.isEmpty(adUnitId)) {
      AdError parameterError =
              MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
                      "Missing or invalid ad Unit ID configured for this ad source instance in the"
                              + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    if (TextUtils.isEmpty(placementId)) {
      AdError parameterError =
              MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS,
                      "Missing or invalid Placement ID configured for this ad source instance in the"
                              + " AdMob or Ad Manager UI.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }

  @Nullable
  public static AdError validateMintegralAdLoadParams(
          @Nullable String adUnitId,
          @Nullable String placementId,
          @Nullable String bidToken) {
    AdError parameterError = validateMintegralAdLoadParams(adUnitId, placementId);
    if (parameterError != null) {
      return parameterError;
    }
    if (TextUtils.isEmpty(bidToken)) {
      parameterError = MintegralConstants.createAdapterError(
              MintegralConstants.ERROR_INVALID_BID_RESPONSE,
              "Missing or invalid Mintegral bidding signal in this ad request.");
      Log.e(TAG, parameterError.toString());
      return parameterError;
    }
    return null;
  }
}
