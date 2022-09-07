package com.google.ads.mediation.mintegral;

import android.os.Bundle;

public class MintegralUtils {
  /**
   * Retrieves whether or not to mute the ad that is about to be rendered.
   */
  public static boolean shouldMuteAudio(Bundle networkExtras) {
    return networkExtras != null && networkExtras.getBoolean(MintegralExtras.Keys.MUTE_AUDIO);
  }
}
