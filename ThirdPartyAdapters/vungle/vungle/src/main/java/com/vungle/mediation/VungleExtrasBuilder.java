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
    static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
    static final String EXTRA_PLAY_PLACEMENT_INDEX = "playPlacement";

    private final Bundle mBundle = new Bundle();
    private String[] mAllPlacements = new String[0];

    public VungleExtrasBuilder(@NonNull @Size(min = 1L) String[] placements) {
        mAllPlacements = placements;
        mBundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
    }

    public VungleExtrasBuilder setPlayingPlacement(@NonNull String placement) {
        for (int i = 0; i < mAllPlacements.length; i++) {
            if (placement.equals(mAllPlacements[i])) {
                mBundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, i);
                return this;
            }
        }
        mBundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, 0);
        return this;
    }

    public VungleExtrasBuilder setPlayingPlacement(int placementNum) {
        if (placementNum < mAllPlacements.length) {
            mBundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, placementNum);
            return this;
        }
        mBundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, 0);
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

    public Bundle build() {
        return mBundle;
    }

    static AdConfig adConfigWithNetworkExtras(Bundle networkExtras) {
        AdConfig adConfig = new AdConfig();
        if (networkExtras != null) {
            adConfig.setIncentivizedUserId(networkExtras.getString(EXTRA_USER_ID));
            adConfig.setSoundEnabled(networkExtras.getBoolean(EXTRA_SOUND_ENABLED, true));
        }
        return adConfig;
    }
}
