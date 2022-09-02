package com.mintegral.mediation;

import android.os.Bundle;

import androidx.annotation.NonNull;

public final class MintegralExtrasBuilder {
    public static final String MUTE_AUDIO = "mute_audio";
    private boolean muteAudio;

    @NonNull
    public MintegralExtrasBuilder setMuteAudio(boolean muteAudio) {
        this.muteAudio = muteAudio;
        return this;
    }

    @NonNull
    public Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(MUTE_AUDIO, muteAudio);
        return bundle;
    }
}
