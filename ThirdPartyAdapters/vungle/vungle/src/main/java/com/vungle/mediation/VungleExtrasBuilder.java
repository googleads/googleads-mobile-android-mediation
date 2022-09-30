package com.vungle.mediation;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.vungle.ads.AdConfig;

/**
 * A helper class for creating a network extras bundle that can be passed to the adapter to make
 * customizations specific to Vungle.
 */
public final class VungleExtrasBuilder {

  public static final String EXTRA_USER_ID = "userId";
  private static final String EXTRA_ORDINAL_VIEW_COUNT = "ordinalViewCount";
  private static final String EXTRA_ORIENTATION = "adOrientation";
  static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
  static final String EXTRA_PLAY_PLACEMENT = "playPlacement";

  private final Bundle bundle = new Bundle();

  public VungleExtrasBuilder(@Nullable @Size(min = 1L) String[] placements) {
    bundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
  }

  public VungleExtrasBuilder setUserId(String userId) {
    bundle.putString(EXTRA_USER_ID, userId);
    return this;
  }

  public VungleExtrasBuilder setOrdinalViewCount(int ordinalViewCount) {
    bundle.putInt(EXTRA_ORDINAL_VIEW_COUNT, ordinalViewCount);
    return this;
  }

  public VungleExtrasBuilder setAdOrientation(int adOrientation) {
    bundle.putInt(EXTRA_ORIENTATION, adOrientation);
    return this;
  }

  public Bundle build() {
    return bundle;
  }

  public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras) {
    AdConfig adConfig = new AdConfig();

    if (networkExtras != null) {
      //TODO TBD adConfig.setOrdinal(networkExtras.getInt(EXTRA_ORDINAL_VIEW_COUNT, 0));
      adConfig.setAdOrientation(networkExtras.getInt(EXTRA_ORIENTATION, AdConfig.AUTO_ROTATE));
    }
    return adConfig;
  }

  public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras,
      NativeAdOptions options) {
    AdConfig adConfig = adConfigWithNetworkExtras(networkExtras);

    int privacyIconPlacement;
    if (options != null) {
      privacyIconPlacement = options.getAdChoicesPlacement();
    } else {
      privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;
    }

    int adOptionsPosition;
    switch (privacyIconPlacement) {
      case NativeAdOptions.ADCHOICES_TOP_LEFT:
        adOptionsPosition = AdConfig.TOP_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
        adOptionsPosition = AdConfig.BOTTOM_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
        adOptionsPosition = AdConfig.BOTTOM_RIGHT;
        break;
      case NativeAdOptions.ADCHOICES_TOP_RIGHT:
      default:
        adOptionsPosition = AdConfig.TOP_RIGHT;
        break;
    }

    adConfig.setAdOptionsPosition(adOptionsPosition);

    return adConfig;
  }
}
