// Copyright 2023 Google LLC
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

package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;

public class PlacementFinder {

  private static final String PLAYING_PLACEMENT = "placementID";

  @Nullable
  public static String findPlacement(Bundle networkExtras, Bundle serverParameters) {
    String placement = null;
    if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
      placement = serverParameters.getString(PLAYING_PLACEMENT);
    }
    if (placement == null) {
      Log.e(TAG, "Missing or invalid placement ID configured for this ad source instance "
          + "in the AdMob or Ad Manager UI.");
    } else {
      Log.d(TAG, "Find the placement prepare to load: " + placement);
    }
    return placement;
  }
}
