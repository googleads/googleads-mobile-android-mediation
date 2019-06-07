package com.jirbo.adcolony;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyUserMetadata;
import com.google.ads.mediation.adcolony.AdColonyAdapterUtils;
import com.google.ads.mediation.adcolony.AdColonyMediationAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * A helper class used by the {@link AdColonyAdapter}.
 */
public class AdColonyManager {
    private static final String TAG = AdColonyAdapter.class.getSimpleName();

    private static AdColonyManager _instance = null;
    private ArrayList<String> configuredZones;
    private boolean isConfigured = false;

    private AdColonyManager() {
        this.configuredZones = new ArrayList<>();
    }

    public static AdColonyManager getInstance() {
        if (_instance == null) {
            _instance = new AdColonyManager();
        }
        return _instance;
    }

    private boolean configureAdColony(Context context,
                                      AdColonyAppOptions options,
                                      String appID,
                                      ArrayList<String> zones) {

        if (!(context instanceof Activity || context instanceof Application)) {
            Log.w(TAG, "Context must be of type Activity or Application.");
            return false;
        }

        if (TextUtils.isEmpty(appID)) {
            Log.w(TAG, "A valid appId wasn't provided.");
            return false;
        }

        if (zones == null || zones.isEmpty()) {
            Log.w(TAG, "No zones provided to request ad.");
            return false;
        }

        // Check to see if the stored list of zones is missing any values.
        for (String zone : zones) {
            if (!configuredZones.contains(zone)) {
                // Not contained in our list.
                configuredZones.add(zone);
                isConfigured = false;
            }
        }
        if (isConfigured) {
            AdColony.setAppOptions(options);
        } else {
            // We are requesting zones that we haven't configured with yet.
            String[] zoneArray = configuredZones.toArray(new String[0]);

            // Always set mediation network info.
            options.setMediationNetwork(AdColonyAppOptions.ADMOB, BuildConfig.VERSION_NAME);
            isConfigured = context instanceof Activity
                    ? AdColony.configure((Activity) context, options, appID, zoneArray)
                    : AdColony.configure((Application) context, options, appID, zoneArray);
        }
        return isConfigured;
    }

    boolean configureAdColony(Context context,
                              Bundle serverParams,
                              MediationAdRequest adRequest,
                              Bundle networkExtras) {
        String appId = serverParams.getString(AdColonyAdapterUtils.KEY_APP_ID);
        ArrayList<String> newZoneList = parseZoneList(serverParams);
        AdColonyAppOptions appOptions = buildAppOptions(adRequest);
        return configureAdColony(context, appOptions, appId, newZoneList);
    }

    public boolean configureAdColony(MediationRewardedAdConfiguration adConfiguration) {
        Context context = adConfiguration.getContext();
        Bundle serverParams = adConfiguration.getServerParameters();
        String appId = serverParams.getString(AdColonyAdapterUtils.KEY_APP_ID);
        ArrayList<String> newZoneList = parseZoneList(serverParams);
        AdColonyAppOptions appOptions = buildAppOptions(adConfiguration);
        return configureAdColony(context, appOptions, appId, newZoneList);
    }

    /**
     * Places user_id, age, location, and gender into AdColonyAppOptions.
     *
     * @param adRequest request received from AdMob.
     * @return a valid AppOptions object.
     */
    private AdColonyAppOptions buildAppOptions(MediationAdRequest adRequest) {
        AdColonyAppOptions options = AdColonyMediationAdapter.getAppOptions();

        if (adRequest != null) {
            // Enable test ads from AdColony when a Test Ad Request was sent.
            if (adRequest.isTesting()) {
                options.setTestModeEnabled(true);
            }

            AdColonyUserMetadata userMetadata = new AdColonyUserMetadata();

            // Try to update userMetaData with gender field.
            int genderVal = adRequest.getGender();
            if (genderVal == AdRequest.GENDER_FEMALE) {
                userMetadata.setUserGender(AdColonyUserMetadata.USER_FEMALE);
            } else if (genderVal == AdRequest.GENDER_MALE) {
                userMetadata.setUserGender(AdColonyUserMetadata.USER_MALE);
            }

            // Try to update userMetaData with location (if provided).
            Location location = adRequest.getLocation();
            if (location != null) {
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
                    userMetadata.setUserAge((int) yearsPassed);
                }
            }
            options.setUserMetadata(userMetadata);
        }
        return options;
    }

    /**
     * Places user_id, age, location, and gender into AdColonyAppOptions.
     *
     * @param adConfiguration rewarded ad configuration received from AdMob.
     * @return a valid AppOptions object.
     */
    private AdColonyAppOptions buildAppOptions(MediationRewardedAdConfiguration adConfiguration) {
        AdColonyAppOptions options = AdColonyMediationAdapter.getAppOptions();

        // Enable test ads from AdColony when a Test Ad Request was sent.
        if (adConfiguration.isTestRequest()) {
            options.setTestModeEnabled(true);
        }

        AdColonyUserMetadata userMetadata = new AdColonyUserMetadata();

        // Try to update userMetaData with location (if provided).
        Location location = adConfiguration.getLocation();
        if (location != null) {
            userMetadata.setUserLocation(location);
        }

        options.setUserMetadata(userMetadata);
        return options;
    }

    public ArrayList<String> parseZoneList(Bundle serverParams) {
        ArrayList<String> newZoneList = null;
        if (serverParams != null) {
            String requestedZones = null;
            if (serverParams.getString(AdColonyAdapterUtils.KEY_ZONE_IDS) != null) {
                requestedZones = serverParams.getString(AdColonyAdapterUtils.KEY_ZONE_IDS);
            } else {
                requestedZones = serverParams.getString(AdColonyAdapterUtils.KEY_ZONE_ID);
            }
            if (requestedZones != null) {
                newZoneList = new ArrayList<>(Arrays.asList(requestedZones.split(";")));
            }
        }
        return newZoneList;
    }

    public String getZoneFromRequest(ArrayList<String> serverListOfZones, Bundle adRequestParams) {
        String requestedZone = null;
        if (serverListOfZones != null && !serverListOfZones.isEmpty()) {
            requestedZone = serverListOfZones.get(0);
        }
        if (adRequestParams != null && adRequestParams.getString(AdColonyAdapterUtils.KEY_ZONE_ID) != null) {
            requestedZone = adRequestParams.getString(AdColonyAdapterUtils.KEY_ZONE_ID);
        }
        return requestedZone;
    }
}
