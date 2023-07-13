// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.applovin.mediation;

import android.os.Bundle;

/**
 * The {@link AppLovinExtras} class creates a mediation extras {@link Bundle} for the AppLovin
 * adapter.
 */
public class AppLovinExtras {

  /** Class containing keys for the AppLovin extras {@link Bundle}. */
  public static class Keys {

    private Keys() {}

    public static final String KEY_WATERMARK = "google_watermark";

    public static final String MUTE_AUDIO = "mute_audio";
  }

  /**
   * Convenience class used to build the AppLovin network extras {@link Bundle}.
   */
  public static class Builder {

    private boolean muteAudio;

    /**
     * Use this to mute audio for video ads. Must be set on each ad request.
     */
    public Builder setMuteAudio(boolean muteAudio) {
      this.muteAudio = muteAudio;
      return this;
    }

    /**
     * Builds a {@link Bundle} object with the given inputs.
     */
    public Bundle build() {
      final Bundle extras = new Bundle(1);
      extras.putBoolean(Keys.MUTE_AUDIO, muteAudio);

      return extras;
    }
  }
}
