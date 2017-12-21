package com.vungle.mediation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.vungle.publisher.AdConfig;

/**
 * A helper class for creating a network extras bundle that can be passed to the adapter to make
 * customizations specific to Vungle.
 */
public final class VungleExtrasBuilder {

    static final String EXTRA_USER_ID = "userId";
    static final String EXTRA_SOUND_ENABLED = "soundEnabled";
    static final String EXTRA_FLEXVIEW_CLOSE_TIME = "flexViewCloseTimeInSec";
    static final String EXTRA_ORDINAL_VIEW_COUNT = "ordinalViewCount";
    static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
    static final String EXTRA_PLAY_PLACEMENT = "playPlacement";

    private final Bundle mBundle = new Bundle();
    private String[] mAllPlacements = new String[0];

    public VungleExtrasBuilder(@NonNull @Size(min = 1L) String[] placements) {
        mAllPlacements = placements;
        mBundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
    }

    public VungleExtrasBuilder setPlayingPlacement(@NonNull String placement) {
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

    static AdConfig adConfigWithNetworkExtras(Bundle networkExtras) {
        AdConfig adConfig = new AdConfig();
        if (networkExtras != null) {
            adConfig.setIncentivizedUserId(networkExtras.getString(EXTRA_USER_ID));
            adConfig.setSoundEnabled(networkExtras.getBoolean(EXTRA_SOUND_ENABLED, true));
            adConfig.setFlexViewCloseTimeInSec(networkExtras.getInt(EXTRA_FLEXVIEW_CLOSE_TIME, 0));
            adConfig.setOrdinalViewCount(networkExtras.getInt(EXTRA_ORDINAL_VIEW_COUNT, 0));
        }
        return adConfig;
    }
}
