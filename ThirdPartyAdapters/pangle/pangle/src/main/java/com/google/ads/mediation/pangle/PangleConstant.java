package com.google.ads.mediation.pangle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PangleConstant {
    public static final String PLACEMENT_ID = "placementid";
    public static final String APP_ID = "appid";
    public static final String ERROR_DOMAIN = "com.google.ads.mediation.pangle";
    public static final String PANGLE_SDK_ERROR_DOMAIN = "com.pangle.ads";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {ERROR_INVALID_PLACEMENT,
                    ERROR_BANNER_AD_SIZE_IS_INVALID,
                    ERROR_BID_RESPONSE_IS_INVALID,
            })
    public @interface AdapterError {

    }

    /**
     * placement is null or empty
     */
    public static final int ERROR_INVALID_PLACEMENT = 101;

    /**
     * The ad size could not be obtained, or the ad size is illegal
     */
    public static final int ERROR_BANNER_AD_SIZE_IS_INVALID = 102;

    /**
     * Missing or invalid bidResponse
     */
    public static final int ERROR_BID_RESPONSE_IS_INVALID = 103;

    /**
     * Pangle SDK not initialized, or initialization error.
     */
    public static final int ERROR_SDK_NOT_INIT = 201;

    @NonNull
    public static AdError createAdapterError(@AdapterError int errorCode, @NonNull String errorMessage) {
        return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
    }

    @NonNull
    public static AdError createSdkError(int errorCode, @NonNull String errorMessage) {
        return new AdError(errorCode, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
    }
}
