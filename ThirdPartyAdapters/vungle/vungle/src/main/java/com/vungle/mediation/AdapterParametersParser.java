package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;

/**
 * The {@link AdapterParametersParser} class helps in creating a Vungle network-specific parameters.
 */
class AdapterParametersParser {
    private static final String TAG = VungleManager.class.getSimpleName();

    static class Config {
        String getAppId() {
            return appId;
        }

        private String appId;
    }

    public static Config parse(Bundle networkExtras, Bundle serverParameters) throws IllegalArgumentException {
        String appId = serverParameters.getString("appid");
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "Vungle app ID should be specified!");
            throw new IllegalArgumentException();
        }

        Config ret = new Config();
        ret.appId = appId;
        return ret;
    }
}
