package com.jirbo.adcolony;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyUserMetadata;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * A helper class used by the {@link AdColonyAdapter}.
 */
class AdColonyManager {
    private static AdColonyManager _instance = null;
    private ArrayList<String> _configuredListOfZones;
    private boolean _isConfigured = false;
    boolean rewardedAdsConfigured = false;
    private Context _context = null;

    private AdColonyManager() {
        this._configuredListOfZones = new ArrayList<>();
    }

    static AdColonyManager getInstance() {
        if (_instance == null) {
            _instance = new AdColonyManager();
        }
        return _instance;
    }

    void onDestroy() {
        _context = null;
    }

    boolean configureAdColony(Context context,
                              Bundle serverParams,
                              MediationAdRequest adRequest,
                              Bundle networkExtras) {
        String appId = serverParams.getString("app_id");
        ArrayList<String> newZoneList = parseZoneList(serverParams);
        boolean needToConfigure = false;

        if (context != null) {
            _context = context; // Update the context if its non-null.
        }

        if (_context != null && !(_context instanceof Activity)) {
            Log.w("AdColonyAdapter", "Context must be of type Activity.");
            return false;
        }

        if (appId == null) {
            Log.w("AdColonyAdapter", "A valid appId wasn't provided.");
            return false;
        }

        if (newZoneList == null || newZoneList.isEmpty()) {
            Log.w("AdColonyAdapter", "No zones provided to request ad.");
            return false;
        }

        // Check to see if the stored list of zones is missing any values.
        for (String zone : newZoneList) {
            if (!_configuredListOfZones.contains(zone)) {
                // Not contained in our list.
                _configuredListOfZones.add(zone);
                needToConfigure = true;
            }
        }

        // Update app-options if necessary.
        AdColonyAppOptions appOptions = buildAppOptions(adRequest, networkExtras);

        // We are requesting zones that we haven't configured with yet.
        if (_isConfigured && !needToConfigure) {
            if (appOptions != null) {
                // Always set the appOptions if non-null.
                AdColony.setAppOptions(appOptions);
            }
        } else {
            // Convert _configuredListOfZones into array.
            String[] zones =
                    _configuredListOfZones.toArray(new String[_configuredListOfZones.size()]);

            // Instantiate app options if null so that we can always send mediation network info.
            if (appOptions == null) {
                appOptions = new AdColonyAppOptions();
            }

            // Always set mediation network info.
            appOptions.setMediationNetwork(AdColonyAppOptions.ADMOB, BuildConfig.VERSION_NAME);
            _isConfigured = AdColony.configure((Activity) _context, appOptions, appId, zones);
        }
        return _isConfigured;
    }

    /**
     * Places user_id, age, location, and gender into AdColonyAppOptions.
     *
     * @param adRequest     request received from AdMob.
     * @param networkExtras possible network parameters sent from AdMob.
     * @return a valid AppOptions object or null if nothing valid was passed from AdMob.
     */
    private AdColonyAppOptions buildAppOptions(MediationAdRequest adRequest,
                                               Bundle networkExtras) {
        AdColonyAppOptions options = new AdColonyAppOptions();
        boolean updatedOptions = false;

        if (networkExtras != null) {
            String userId = networkExtras.getString("user_id");
            if (userId != null) {
                options.setUserID(userId);
                updatedOptions = true;
            }
            String npa = networkExtras.getString("npa");
            if (npa != null) {
                options.setOption("explicit_consent_given", true);
                options.setOption("consent_response", !npa.equals("1"));
                updatedOptions = true;
            }
        }

        if (adRequest != null) {
            // Enable test ads from AdColony when a Test Ad Request was sent.
            if (adRequest.isTesting()) {
                options.setTestModeEnabled(true);
                updatedOptions = true;
            }

            // Try to update userMetaData with gender field.
            AdColonyUserMetadata userMetadata = new AdColonyUserMetadata();
            int genderVal = adRequest.getGender();
            if (genderVal == AdRequest.GENDER_FEMALE) {
                updatedOptions = true;
                userMetadata.setUserGender(AdColonyUserMetadata.USER_FEMALE);
            } else if (genderVal == AdRequest.GENDER_MALE) {
                updatedOptions = true;
                userMetadata.setUserGender(AdColonyUserMetadata.USER_MALE);
            }

            // Try to update userMetaData with location (if provided).
            Location location = adRequest.getLocation();
            if (location != null) {
                updatedOptions = true;
                userMetadata.setUserLocation(location);
            }

            // Try to update userMetaData with age if birth date is provided.
            Date birthday = adRequest.getBirthday();
            if (birthday != null) {
                long currentTime = System.currentTimeMillis();
                long birthdayTime = birthday.getTime();
                long diff = currentTime - birthdayTime;
                if (diff > 0) {
                    long day = (1000 * 60 * 60 * 24);
                    long yearsPassed = diff / day / 365;
                    updatedOptions = true;
                    userMetadata.setUserAge((int) yearsPassed);
                }
            }
            options.setUserMetadata(userMetadata);
        }
        if (updatedOptions) {
            return options;
        } else {
            return null;
        }
    }

    ArrayList<String> parseZoneList(Bundle serverParams) {
        ArrayList<String> newZoneList = null;
        if (serverParams != null) {
            String requestedZones = serverParams.getString("zone_ids");
            if (requestedZones != null) {
                newZoneList = new ArrayList<>(Arrays.asList(requestedZones.split(";")));
            }
        }
        return newZoneList;
    }

    String getZoneFromRequest(ArrayList<String> serverListOfZones, Bundle adRequestParams) {
        String requestedZone = null;
        if (serverListOfZones != null && !serverListOfZones.isEmpty()) {
            requestedZone = serverListOfZones.get(0);
        }
        if (adRequestParams != null && adRequestParams.getString("zone_id") != null) {
            requestedZone = adRequestParams.getString("zone_id");
        }
        return requestedZone;
    }
}
