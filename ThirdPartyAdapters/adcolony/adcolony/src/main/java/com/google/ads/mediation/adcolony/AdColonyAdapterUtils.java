package com.google.ads.mediation.adcolony;

import android.content.Context;
import android.content.res.Resources;

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
}
