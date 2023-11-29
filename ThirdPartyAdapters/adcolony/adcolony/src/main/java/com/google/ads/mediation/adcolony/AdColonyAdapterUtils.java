// Copyright 2019 Google LLC
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

package com.google.ads.mediation.adcolony;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAppOptions;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;
import com.jirbo.adcolony.BuildConfig;
import java.util.ArrayList;

public class AdColonyAdapterUtils {

  public static final String KEY_APP_ID = "app_id";
  public static final String KEY_ZONE_ID = "zone_id";
  public static final String KEY_ZONE_IDS = "zone_ids";

  // AdMob SDK's bid response passed to AdColony using below key in ad options.
  public static final String KEY_ADCOLONY_BID_RESPONSE = "adm";

  @Nullable
  public static AdColonyAdSize adColonyAdSizeFromAdMobAdSize(@NonNull Context context,
      @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    potentials.add(AdSize.WIDE_SKYSCRAPER);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);

    if (AdSize.BANNER.equals(closestSize)) {
      return AdColonyAdSize.BANNER;
    } else if (AdSize.MEDIUM_RECTANGLE.equals(closestSize)) {
      return AdColonyAdSize.MEDIUM_RECTANGLE;
    } else if (AdSize.LEADERBOARD.equals(closestSize)) {
      return AdColonyAdSize.LEADERBOARD;
    } else if (AdSize.WIDE_SKYSCRAPER.equals(closestSize)) {
      return AdColonyAdSize.SKYSCRAPER;
    }

    return null;
  }

  /**
   * This method converts device specific pixels to density independent pixels.
   *
   * @param px A value in px (pixels) unit. Which we need to convert into dp
   * @return A int value to represent dp equivalent to px value
   */
  public static int convertPixelsToDp(int px) {
    return (int) (px / Resources.getSystem().getDisplayMetrics().density);
  }

  public static void setCoppaPrivacyFrameworkRequired(@TagForChildDirectedTreatment int coppa) {
    AdColonyAppOptions appOptions = AdColonyMediationAdapter.getAppOptions();
    switch (coppa) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.COPPA, true);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.COPPA, false);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        break;
    }
  }

  static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }
}
