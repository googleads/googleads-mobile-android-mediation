package com.google.ads.mediation.inmobi;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import java.util.HashMap;

public class InMobiExtrasBuilder {
  public static final String THIRD_PARTY_KEY = "tp";

  public static final String THIRD_PARTY_VERSION = "tp-ver";

  public static final String COPPA = "coppa";

  @NonNull
  public static InMobiExtras build(@Nullable Bundle mediationExtras, @NonNull String protocol) {
    HashMap<String, String> map = new HashMap<>();
    // Set keywords as an empty string for now.
    String keywords = "";
    if (mediationExtras != null && mediationExtras.keySet() != null) {
      for (String key : mediationExtras.keySet()) {
        map.put(key, mediationExtras.getString(key));
      }
    }

    map.put(THIRD_PARTY_KEY, protocol);
    map.put(THIRD_PARTY_VERSION, MobileAds.getVersion().toString());
    // If the COPPA value isn't specified by the publisher, InMobi SDK expects the default value to
    // be `0`.
    if (MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment()
        == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      map.put(COPPA, "1");
    } else {
      map.put(COPPA, "0");
    }

    return new InMobiExtras(map, keywords);
  }
}
