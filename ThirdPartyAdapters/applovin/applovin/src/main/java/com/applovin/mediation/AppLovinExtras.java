package com.applovin.mediation;

import android.os.Bundle;

/**
 * The {@link AppLovinExtras} class creates a mediation extras {@link Bundle} for the AppLovin
 * adapter.
 */
public class AppLovinExtras {

  /** Class containing keys for the AppLovin extras {@link Bundle}. */
  static class Keys {

    static final String MUTE_AUDIO = "mute_audio";
  }

  /** Convenience class used to build the AppLovin network extras {@link Bundle}. */
  public static class Builder {

    private boolean mMuteAudio;

    /** Use this to mute audio for video ads. Must be set on each ad request. */
    public Builder setMuteAudio(boolean muteAudio) {
      mMuteAudio = muteAudio;
      return this;
    }

    /** Builds a {@link Bundle} object with the given inputs. */
    public Bundle build() {
      final Bundle extras = new Bundle(1);
      extras.putBoolean(Keys.MUTE_AUDIO, mMuteAudio);

      return extras;
    }
  }
}
