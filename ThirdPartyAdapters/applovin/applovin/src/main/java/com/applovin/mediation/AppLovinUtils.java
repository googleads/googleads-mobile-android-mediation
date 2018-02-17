package com.applovin.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.android.gms.ads.AdRequest;

/**
 * Created by Thomas So on 1/25/18.
 */

class AppLovinUtils
{
    private static final String DEFAULT_ZONE = "";

    /**
     * Keys for retrieving values from the server parameters.
     */
    private static class ServerParameterKeys
    {
        private static final String SDK_KEY   = "sdkKey";
        private static final String PLACEMENT = "placement";
        private static final String ZONE_ID   = "zone_id";
    }

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server parameters, or Android Manifest.
     */
    static AppLovinSdk retrieveSdk(Bundle serverParameters, Context context)
    {
        final String sdkKey = serverParameters.getString( ServerParameterKeys.SDK_KEY );
        final AppLovinSdk sdk;

        if ( !TextUtils.isEmpty( sdkKey ) )
        {
            sdk = AppLovinSdk.getInstance( sdkKey, new AppLovinSdkSettings(), context );
        }
        else
        {
            sdk = AppLovinSdk.getInstance( context );
        }


        sdk.setPluginVersion( BuildConfig.VERSION_NAME );

        return sdk;
    }

    /**
     * Retrieves the placement from an appropriate connector object. Will use empty string if none exists.
     */
    static String retrievePlacement(Bundle serverParameters)
    {
        if ( serverParameters.containsKey( ServerParameterKeys.PLACEMENT ) )
        {
            return serverParameters.getString( ServerParameterKeys.PLACEMENT );
        }
        else
        {
            return null;
        }
    }

    /**
     * Retrieves the zone identifier from an appropriate connector object. Will use empty string if none exists.
     */
    static String retrieveZoneId(Bundle serverParameters)
    {
        if ( serverParameters.containsKey( ServerParameterKeys.ZONE_ID ) )
        {
            return serverParameters.getString( ServerParameterKeys.ZONE_ID );
        }
        else
        {
            return DEFAULT_ZONE;
        }
    }

    /**
     * Retrieves whether or not to mute the ad that is about to be rendered.
     */
    static boolean shouldMuteAudio(Bundle networkExtras)
    {
        return networkExtras != null && networkExtras.getBoolean( AppLovinExtras.Keys.MUTE_AUDIO );
    }

    /**
     * Convert the given AppLovin SDK error code into the appropriate AdMob error code.
     */
    static int toAdMobErrorCode(int applovinErrorCode)
    {
        //
        // TODO: Be more exhaustive
        //

        if ( applovinErrorCode == AppLovinErrorCodes.NO_FILL )
        {
            return AdRequest.ERROR_CODE_NO_FILL;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT )
        {
            return AdRequest.ERROR_CODE_NETWORK_ERROR;
        }
        else
        {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}
