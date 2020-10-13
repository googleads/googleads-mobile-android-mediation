package com.vungle.mediation;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.vungle.warren.AdConfig;
import java.util.UUID;

/**
 * A helper class for creating a network extras bundle that can be passed to the adapter to make
 * customizations specific to Vungle.
 */
public final class VungleExtrasBuilder {

  public static final String EXTRA_USER_ID = "userId";
  private static final String EXTRA_START_MUTED = "startMuted";
  private static final String EXTRA_ORDINAL_VIEW_COUNT = "ordinalViewCount";
  private static final String EXTRA_ORIENTATION = "adOrientation";
  static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
  static final String EXTRA_PLAY_PLACEMENT = "playPlacement";
  static final String UUID_KEY = "uniqueVungleRequestKey";

  private final Bundle mBundle = new Bundle();

  public VungleExtrasBuilder(@Nullable @Size(min = 1L) String[] placements) {
    mBundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
  }

  public VungleExtrasBuilder setPlayingPlacement(String placement) {
    mBundle.putString(EXTRA_PLAY_PLACEMENT, placement);
    return this;
  }

  @Deprecated
  public VungleExtrasBuilder setSoundEnabled(boolean enabled) {
    return setStartMuted(!enabled);
  }

  public VungleExtrasBuilder setStartMuted(boolean muted) {
    mBundle.putBoolean(EXTRA_START_MUTED, muted);
    return this;
  }

  public VungleExtrasBuilder setUserId(String userId) {
    mBundle.putString(EXTRA_USER_ID, userId);
    return this;
  }

  public VungleExtrasBuilder setOrdinalViewCount(int ordinalViewCount) {
    mBundle.putInt(EXTRA_ORDINAL_VIEW_COUNT, ordinalViewCount);
    return this;
  }

  public VungleExtrasBuilder setAdOrientation(int adOrientation) {
    mBundle.putInt(EXTRA_ORIENTATION, adOrientation);
    return this;
  }

  public VungleExtrasBuilder setBannerUniqueRequestID(String uniqueID) {
    mBundle.putString(UUID_KEY, uniqueID);
    return this;
  }

  public Bundle build() {
    if (TextUtils.isEmpty(mBundle.getString(UUID_KEY, null))) {
      mBundle.putString(UUID_KEY, UUID.randomUUID().toString());
    }
    return mBundle;
  }

  public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras, boolean defaultMuteState) {
    AdConfig adConfig = new AdConfig();
    adConfig.setMuted(defaultMuteState);

    if (networkExtras != null) {
      adConfig.setMuted(networkExtras.getBoolean(EXTRA_START_MUTED, defaultMuteState));
      adConfig.setOrdinal(networkExtras.getInt(EXTRA_ORDINAL_VIEW_COUNT, 0));
      adConfig.setAdOrientation(networkExtras.getInt(EXTRA_ORIENTATION, AdConfig.AUTO_ROTATE));
    }
    return adConfig;
  }
}
