package com.google.ads.mediation.mintegral;
import android.os.Bundle;

import androidx.annotation.NonNull;

public final class MintegralExtras {
  static class Keys {
    static final String MUTE_AUDIO = "mute_audio";
  }

  public static class Builder {
    private boolean muteAudio;

    @NonNull
    public Builder setMuteAudio(boolean muteAudio) {
      this.muteAudio = muteAudio;
      return this;
    }

    @NonNull
    public Bundle build() {
      Bundle extras = new Bundle();
      extras.putBoolean(Keys.MUTE_AUDIO, muteAudio);
      return extras;
    }
  }
}