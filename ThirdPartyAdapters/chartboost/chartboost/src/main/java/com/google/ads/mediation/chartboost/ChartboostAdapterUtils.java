package com.google.ads.mediation.chartboost;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Model.CBError;
import com.google.android.gms.ads.AdRequest;

/**
 * Utility methods for the Chartboost Adapter.
 */
class ChartboostAdapterUtils {

    /**
     * Key to obtain App ID, required for initializing Chartboost SDK.
     */
    static final String KEY_APP_ID = "appId";

    /**
     * Key to obtain App Signature, required for initializing Charboost SDK.
     */
    static final String KEY_APP_SIGNATURE = "appSignature";

    /**
     * Key to obtain Ad Location. This is added in adapter version 1.1.0.
     */
    static final String KEY_AD_LOCATION = "adLocation";

    /**
     * Creates and return a new {@link ChartboostParams} object populated with the parameters
     * obtained from the server parameters and network extras bundles.
     *
     * @param serverParameters a {@link Bundle} containing server parameters used to initialize
     *                         Chartboost.
     * @param networkExtras    a {@link Bundle} containing optional information to be used by the
     *                         adapter.
     * @return a {@link ChartboostParams} object populated with the params obtained from the
     * bundles provided.
     */
    static ChartboostParams createChartboostParams(Bundle serverParameters, Bundle networkExtras) {
        ChartboostParams params = new ChartboostParams();
        String appId = serverParameters.getString(KEY_APP_ID);
        String appSignature = serverParameters.getString(KEY_APP_SIGNATURE);
        if (appId != null && appSignature != null) {
            params.setAppId(appId.trim());
            params.setAppSignature(appSignature.trim());
        }

        String adLocation = serverParameters.getString(KEY_AD_LOCATION);
        if (!isValidParam(adLocation)) {
            // Ad Location is empty, log a warning and use the default location.
            String logMessage = String.format("Chartboost ad location is empty, defaulting to %s. "
                            + "Please set the Ad Location parameter in the AdMob UI.",
                    CBLocation.LOCATION_DEFAULT);
            Log.w(ChartboostMediationAdapter.TAG, logMessage);
            adLocation = CBLocation.LOCATION_DEFAULT;
        }
        params.setLocation(adLocation.trim());

        if (networkExtras != null) {
            if (networkExtras.containsKey(ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK)
                    && networkExtras.containsKey(
                    ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION)) {
                params.setFramework((Chartboost.CBFramework) networkExtras.getSerializable(
                        ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK));
                params.setFrameworkVersion(networkExtras.getString(
                        ChartboostAdapter.ChartboostExtrasBundleBuilder.KEY_FRAMEWORK_VERSION));
            }
        }
        return params;
    }

    /**
     * Checks whether or not the provided {@link ChartboostParams} is valid.
     *
     * @param params Chartboost params to be examined.
     * @return {@code true} if the given ChartboostParams' appId and appSignature are valid,
     * false otherwise.
     */
    static boolean isValidChartboostParams(ChartboostParams params) {
        String appId = params.getAppId();
        String appSignature = params.getAppSignature();
        if (!isValidParam(appId) || !isValidParam(appSignature)) {
            String log = !isValidParam(appId) ? (!isValidParam(appSignature)
                    ? "App ID and App Signature" : "App ID") : "App Signature";
            Log.w(ChartboostMediationAdapter.TAG, log + " cannot be empty.");
            return false;
        }
        return true;
    }

    /**
     * Checks whether or not the Chartboost parameter string provided is valid.
     *
     * @param string the string to be examined.
     * @return {@code true} if the param string is not null and length when trimmed is not
     * zero, {@code false} otherwise.
     */
    static boolean isValidParam(String string) {
        return !(string == null || string.trim().length() == 0);
    }

    /**
     * Chartboost requires an Activity context to Initialize. This method will return false if
     * the context provided is either null or is not an Activity context.
     *
     * @param context to be checked if it is valid.
     * @return {@code true} if the context provided is valid, {@code false} otherwise.
     */
    static boolean isValidContext(Context context) {
        if (context == null) {
            Log.w(ChartboostAdapter.TAG, "Context cannot be null");
            return false;
        }

        if (!(context instanceof Activity)) {
            Log.w(ChartboostAdapter.TAG,
                    "Context is not an Activity. " +
                            "Chartboost requires an Activity context to load ads.");
            return false;
        }
        return true;
    }

    /**
     * Converts a Chartboost SDK error code to a Google Mobile Ads SDK error code.
     *
     * @param error CBImpressionError type to be translated to Google Mobile Ads SDK readable
     *              error code.
     * @return Ad request error code.
     */
    static int getAdRequestErrorType(CBError.CBImpressionError error) {
        switch (error) {
            case INTERNAL:
            case INVALID_RESPONSE:
            case NO_HOST_ACTIVITY:
            case USER_CANCELLATION:
            case WRONG_ORIENTATION:
            case ERROR_PLAYING_VIDEO:
            case ERROR_CREATING_VIEW:
            case SESSION_NOT_STARTED:
            case ERROR_DISPLAYING_VIEW:
            case ERROR_LOADING_WEB_VIEW:
            case INCOMPATIBLE_API_VERSION:
            case ASSET_PREFETCH_IN_PROGRESS:
            case IMPRESSION_ALREADY_VISIBLE:
            case ACTIVITY_MISSING_IN_MANIFEST:
            case WEB_VIEW_CLIENT_RECEIVED_ERROR:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
            case NETWORK_FAILURE:
            case END_POINT_DISABLED:
            case INTERNET_UNAVAILABLE:
            case TOO_MANY_CONNECTIONS:
            case ASSETS_DOWNLOAD_FAILURE:
            case WEB_VIEW_PAGE_LOAD_TIMEOUT:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;
            case INVALID_LOCATION:
            case VIDEO_ID_MISSING:
            case HARDWARE_ACCELERATION_DISABLED:
            case FIRST_SESSION_INTERSTITIALS_DISABLED:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;
            case INTERNET_UNAVAILABLE_AT_SHOW:
            case NO_AD_FOUND:
            case ASSET_MISSING:
            case VIDEO_UNAVAILABLE:
            case EMPTY_LOCAL_VIDEO_LIST:
            case PENDING_IMPRESSION_ERROR:
            case VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION:
            default:
                return AdRequest.ERROR_CODE_NO_FILL;
        }
    }
}
