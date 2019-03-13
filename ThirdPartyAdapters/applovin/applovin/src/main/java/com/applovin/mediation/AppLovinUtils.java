package com.applovin.mediation;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;

/*
 * A helper class used by {@link AppLovinAdapter}.
 */
public class AppLovinUtils {
    private static final String DEFAULT_ZONE = "";

    private static final int BANNER_STANDARD_HEIGHT = 50;
    private static final int BANNER_HEIGHT_OFFSET_TOLERANCE = 10;

    /**
     * Keys for retrieving values from the server parameters.
     */
    private static class ServerParameterKeys {
        private static final String SDK_KEY = "sdkKey";
        private static final String PLACEMENT = "placement";
        private static final String ZONE_ID = "zone_id";
    }

    /**
     * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server
     * parameters, or Android Manifest.
     */
    public static AppLovinSdk retrieveSdk(Bundle serverParameters, Context context) {
        final String sdkKey = (serverParameters != null) ?
                serverParameters.getString(ServerParameterKeys.SDK_KEY) : null;

        final AppLovinSdk sdk;

        if (!TextUtils.isEmpty(sdkKey)) {
            sdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(), context);
        } else {
            sdk = AppLovinSdk.getInstance(context);
        }

        sdk.setPluginVersion(BuildConfig.VERSION_NAME);
        sdk.setMediationProvider(AppLovinMediationProvider.ADMOB);

        return sdk;
    }

    /**
     * Checks whether or not the Android Manifest has a valid SDK key
     */
    public static boolean androidManifestHasValidSdkKey(Context context) {
        final Bundle metaData = retrieveMetadata(context);
        if (metaData != null) {
            final String sdkKey = metaData.getString("applovin.sdk.key");
            return !TextUtils.isEmpty(sdkKey);
        }

        return false;
    }

    private static Bundle retrieveMetadata(Context context) {
        try {
            final PackageManager pm = context.getPackageManager();
            final ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);

            return ai.metaData;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return null;
    }

    /**
     * Retrieves the placement from an appropriate connector object. Will use empty string if none
     * exists.
     */
    public static String retrievePlacement(Bundle serverParameters) {
        if (serverParameters.containsKey(ServerParameterKeys.PLACEMENT)) {
            return serverParameters.getString(ServerParameterKeys.PLACEMENT);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the zone identifier from an appropriate connector object. Will use empty string if
     * none exists.
     */
    public static String retrieveZoneId(Bundle serverParameters) {
        if (serverParameters.containsKey(ServerParameterKeys.ZONE_ID)) {
            return serverParameters.getString(ServerParameterKeys.ZONE_ID);
        } else {
            return DEFAULT_ZONE;
        }
    }

    /**
     * Retrieves whether or not to mute the ad that is about to be rendered.
     */
    public static boolean shouldMuteAudio(Bundle networkExtras) {
        return networkExtras != null && networkExtras.getBoolean(AppLovinExtras.Keys.MUTE_AUDIO);
    }

    /**
     * Convert the given AppLovin SDK error code into the appropriate AdMob error code.
     */
    public static int toAdMobErrorCode(int applovinErrorCode) {
        //
        // TODO: Be more exhaustive
        //
        if (applovinErrorCode == AppLovinErrorCodes.NO_FILL) {
            return AdRequest.ERROR_CODE_NO_FILL;
        } else if (applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT) {
            return AdRequest.ERROR_CODE_NETWORK_ERROR;
        } else {
            return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }

    /**
     * Get the {@link AppLovinAdSize} from a given {@link AdSize} from AdMob.
     */
    public static AppLovinAdSize appLovinAdSizeFromAdMobAdSize(AdSize adSize) {
        final boolean isSmartBanner = (adSize.getWidth() == AdSize.FULL_WIDTH) &&
                (adSize.getHeight() == AdSize.AUTO_HEIGHT);

        if (AdSize.BANNER.equals(adSize) || AdSize.LARGE_BANNER.equals(adSize) || isSmartBanner) {
            return AppLovinAdSize.BANNER;
        } else if (AdSize.MEDIUM_RECTANGLE.equals(adSize)) {
            return AppLovinAdSize.MREC;
        } else if (AdSize.LEADERBOARD.equals(adSize)) {
            return AppLovinAdSize.LEADER;
        }

        // Assume fluid width, and check for height with offset tolerance
        final int offset = Math.abs(BANNER_STANDARD_HEIGHT - adSize.getHeight());
        if (offset <= BANNER_HEIGHT_OFFSET_TOLERANCE) {
            return AppLovinAdSize.BANNER;
        }
        return null;
    }
}
