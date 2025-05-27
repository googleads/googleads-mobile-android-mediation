// Copyright 2020 Google LLC
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

package com.google.ads.mediation.fyber;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;

/** Utility class for the DT Exchange adapter. */
class FyberAdapterUtils {

  /** Private constructor */
  private FyberAdapterUtils() {}

  @NonNull
  public static String getSdkVersion() {
    return InneractiveAdManager.getVersion();
  }

  @NonNull
  public static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }

  /**
   * Extract age from mediation extras and add it to DT Exchange SDK's global user params setting.
   *
   * @param mediationExtras mediation extras bundle
   */
  static void updateFyberExtraParams(@Nullable Bundle mediationExtras) {
    if (mediationExtras == null) {
      return;
    }

    InneractiveUserConfig userParams = new InneractiveUserConfig();
    if (mediationExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
      int age = mediationExtras.getInt(InneractiveMediationDefs.KEY_AGE, 0);
      userParams.setAge(age);
    }
    InneractiveAdManager.setUserParams(userParams);

    if (mediationExtras.containsKey(FyberMediationAdapter.KEY_MUTE_VIDEO)) {
      boolean muteState = mediationExtras.getBoolean(FyberMediationAdapter.KEY_MUTE_VIDEO, false);
      InneractiveAdManager.setMuteVideo(muteState);
    }
  }
}
