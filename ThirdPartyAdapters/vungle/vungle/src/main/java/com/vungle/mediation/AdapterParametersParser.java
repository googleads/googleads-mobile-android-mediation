package com.vungle.mediation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by andrey on 7/27/17.
 */

class AdapterParametersParser {
    private static final String TAG = VungleManager.class.getSimpleName();

    static class Config {
        String getAppId() {
            return appId;
        }

        String[] getAllPlacements() {
            return allPlacements;
        }

        private String appId;
        private String[] allPlacements;
    }

    public static Config parse(Bundle networkExtras, Bundle serverParameters) throws IllegalArgumentException {
        String[] placements = null;
        if (networkExtras != null)
            placements = networkExtras.getStringArray(VungleExtrasBuilder.EXTRA_ALL_PLACEMENTS);

        String appId = serverParameters.getString("appid");
        if (appId == null || appId.isEmpty()) {
            Log.e(TAG, "Vungle app ID should be specified!");
            throw new IllegalArgumentException();
        }

        if (appId.contains(VungleManager.PLACEMENTS_DELIMITER)) {
            String[] parts = appId.split(VungleManager.PLACEMENTS_DELIMITER);
            appId = parts[0];
            HashSet<String> allPlacements = new HashSet<>();
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty())
                    allPlacements.add(parts[i]);
            }
            if (allPlacements.size() > 0 && placements != null)
                Log.i(TAG, "'allPlacements' had a value in both serverParameters and networkExtras. Used combined value");
            if (placements != null)
                allPlacements.addAll(Arrays.asList(placements));
            placements = allPlacements.toArray(new String[allPlacements.size()]);
        }


        if (placements == null || placements.length == 0) {
            Log.e(TAG, "At least one placement should be specified!");
            throw new IllegalArgumentException();
        }
        Config ret = new Config();
        ret.appId = appId;
        ret.allPlacements = placements;
        return ret;
    }
}
