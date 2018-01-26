package com.applovin.mediation;

import android.os.Bundle;

/**
 * Created by Thomas So on 1/25/18.
 */
public class AppLovinExtras
{
    /**
     * Class containing keys for the AppLovin extras {@link Bundle}.
     */
    static class Keys
    {
        static final String MUTE_AUDIO = "mute_audio";
        static final String ZONE_ID    = "zone_id";
    }

    /**
     * Convenience class used to build the AppLovin network extras {@link Bundle}.
     */
    public class Builder
    {
        private boolean mMuteAudio;
        private String  mZoneId;

        /**
         * Use this to mute audio for video ads. Must be set on each ad request.
         */
        public Builder setMuteAudio(boolean muteAudio)
        {
            mMuteAudio = muteAudio;
            return this;
        }

        /**
         * The accompanying zone identifier with this ad request, if any.
         */
        public Builder setZoneId(String zoneId)
        {
            mZoneId = zoneId;
            return this;
        }

        /**
         * Builds a {@link Bundle} object with the given inputs.
         */
        public Bundle build()
        {
            final Bundle extras = new Bundle();
            extras.putBoolean( Keys.MUTE_AUDIO, mMuteAudio );
            extras.putString( Keys.ZONE_ID, mZoneId );

            return extras;
        }
    }
}
