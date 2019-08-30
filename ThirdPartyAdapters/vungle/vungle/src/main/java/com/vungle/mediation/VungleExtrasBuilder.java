package com.vungle.mediation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.vungle.warren.AdConfig;

/**
 * A helper class for creating a network extras bundle that can be passed to the adapter to make
 * customizations specific to Vungle.
 */
public final class VungleExtrasBuilder {

    public static final String EXTRA_USER_ID = "userId";
    private static final String EXTRA_SOUND_ENABLED = "soundEnabled";
    private static final String EXTRA_FLEXVIEW_CLOSE_TIME = "flexViewCloseTimeInSec";
    private static final String EXTRA_ORDINAL_VIEW_COUNT = "ordinalViewCount";
    static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
    static final String EXTRA_PLAY_PLACEMENT = "playPlacement";

    private final Bundle mBundle = new Bundle();

    public VungleExtrasBuilder(@Nullable @Size(min = 1L) String[] placements) {
        mBundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
    }

    public VungleExtrasBuilder setPlayingPlacement(String placement) {
        mBundle.putString(EXTRA_PLAY_PLACEMENT, placement);
        return this;
    }

    public VungleExtrasBuilder setSoundEnabled(boolean enabled) {
        mBundle.putBoolean(EXTRA_SOUND_ENABLED, enabled);
        return this;
    }

    public VungleExtrasBuilder setUserId(String userId) {
        mBundle.putString(EXTRA_USER_ID, userId);
        return this;
    }

    public VungleExtrasBuilder setFlexViewCloseTimeInSec(int flexViewCloseTimeInSec) {
        mBundle.putInt(EXTRA_FLEXVIEW_CLOSE_TIME, flexViewCloseTimeInSec);
        return this;
    }

    public VungleExtrasBuilder setOrdinalViewCount(int ordinalViewCount) {
        mBundle.putInt(EXTRA_ORDINAL_VIEW_COUNT, ordinalViewCount);
        return this;
    }

    public Bundle build() {
        return mBundle;
    }

    public static AdConfig adConfigWithNetworkExtras(Bundle networkExtras) {
        AdConfig adConfig = new AdConfig();
        if (networkExtras != null) {
            adConfig.setMuted(!networkExtras.getBoolean(EXTRA_SOUND_ENABLED, true));
            adConfig.setFlexViewCloseTime(networkExtras.getInt(EXTRA_FLEXVIEW_CLOSE_TIME, 0));
            adConfig.setOrdinal(networkExtras.getInt(EXTRA_ORDINAL_VIEW_COUNT, 0));
        }
        return adConfig;
    }
}
