package com.applovin.mediation;

import android.os.Bundle;

/**
 * Extras class allowing users to mute video audio if desired
 */
public class AppLovinExtrasBundleBuilder {
    static final String MUTE_AUDIO = "muteAudio";

    /**
     * An extra value used to mute the audio for video ads
     */
    private static boolean muteAudio;

    public static void setMuteAudio(boolean mute)
    {
        muteAudio = mute;
    }

    public static Bundle build() {
        Bundle extras = new Bundle();
        extras.putBoolean( MUTE_AUDIO, muteAudio );
        return extras;
    }
}