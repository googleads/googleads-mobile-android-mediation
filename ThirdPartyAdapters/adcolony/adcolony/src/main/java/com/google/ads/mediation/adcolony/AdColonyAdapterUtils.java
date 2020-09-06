package com.google.ads.mediation.adcolony;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adcolony.sdk.AdColonyAdSize;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import java.util.ArrayList;

public class AdColonyAdapterUtils {

  public static final String KEY_APP_ID = "app_id";
  public static final String KEY_ZONE_ID = "zone_id";
  public static final String KEY_ZONE_IDS = "zone_ids";

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

}
