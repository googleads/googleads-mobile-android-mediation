package com.vungle.mediation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.vungle.publisher.AdConfig;

public final class VungleExtrasBuilder {

    static final String EXTRA_USER_ID = "userId";
    static final String EXTRA_SOUND_ENABLED = "soundEnabled";
    static final String EXTRA_ALL_PLACEMENTS = "allPlacements";
    static final String EXTRA_PLAY_PLACEMENT_INDEX = "playPlacement";

    private final Bundle bundle = new Bundle();
    private String[] allPlacements = new String[0];

    public VungleExtrasBuilder(@NonNull @Size(min = 1L) String[] placements) {
        allPlacements = placements;
        bundle.putStringArray(EXTRA_ALL_PLACEMENTS, placements);
    }

    public VungleExtrasBuilder setPlayingPlacement(@NonNull String placement) {
        for (int i = 0; i < allPlacements.length; i++) {
            if (placement.equals(allPlacements[i])) {
                bundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, i);
                return this;
            }
        }
        bundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, 0);
        return this;
    }

    public VungleExtrasBuilder setPlayingPlacement(int placementNum) {
        if (placementNum < allPlacements.length) {
            bundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, placementNum);
            return this;
        }
        bundle.putInt(EXTRA_PLAY_PLACEMENT_INDEX, 0);
        return this;
    }

    public VungleExtrasBuilder setSoundEnabled(boolean enabled) {
        bundle.putBoolean(EXTRA_SOUND_ENABLED, enabled);
        return this;
    }

    public VungleExtrasBuilder setUserId(String userId) {
        bundle.putString(EXTRA_USER_ID, userId);
        return this;
    }

    public Bundle build() {
        return bundle;
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
