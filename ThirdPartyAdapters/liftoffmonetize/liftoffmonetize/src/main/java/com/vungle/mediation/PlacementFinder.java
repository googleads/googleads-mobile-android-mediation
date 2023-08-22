package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_PLAY_PLACEMENT;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;

public class PlacementFinder {

  private static final String PLAYING_PLACEMENT = "placementID";

  @Nullable
  public static String findPlacement(Bundle networkExtras, Bundle serverParameters) {
    String placement = null;
    if (networkExtras != null
        && networkExtras.containsKey(KEY_PLAY_PLACEMENT)) {
      placement = networkExtras.getString(KEY_PLAY_PLACEMENT);
    }
    if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
      if (placement != null) {
        Log.i(
            TAG,
            "'placementID' had a value in both serverParameters and networkExtras. "
                + "Used one from serverParameters");
      }
      placement = serverParameters.getString(PLAYING_PLACEMENT);
    }
    if (placement == null) {
      Log.e(TAG, "placementID not provided from serverParameters.");
    }
    return placement;
  }
}
