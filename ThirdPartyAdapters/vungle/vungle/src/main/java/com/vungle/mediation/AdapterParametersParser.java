package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;

/**
 * The {@link AdapterParametersParser} class helps in creating a Vungle network-specific parameters.
 */
class AdapterParametersParser {
    private static final String TAG = VungleManager.class.getSimpleName();

    static class Config {
        private String appId;
        private String requestUniqueId;

        public String getAppId() {
            return appId;
        }

        public String getRequestUniqueId() {
            return requestUniqueId;
        }

    }

    public static Config parse(Bundle networkExtras, Bundle serverParameters) throws IllegalArgumentException {
        String appId = serverParameters.getString("appid");
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "Vungle app ID should be specified!");
            throw new IllegalArgumentException();
        }

        String uuid = null;
        if (networkExtras != null && networkExtras.containsKey(VungleExtrasBuilder.UUID_KEY)) {
            uuid = networkExtras.getString(VungleExtrasBuilder.UUID_KEY);
        }

        Config ret = new Config();
        ret.appId = appId;
        ret.requestUniqueId = uuid;
        return ret;
    }
}
