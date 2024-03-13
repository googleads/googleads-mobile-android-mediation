// Copyright 2022 Google LLC
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