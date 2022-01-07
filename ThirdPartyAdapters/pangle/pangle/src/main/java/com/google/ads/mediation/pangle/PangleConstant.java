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
                    ERROR_SHOW_FAIL,
                    ERROR_BANNER_AD_SIZE_IS_NULL,
            })
    public @interface AdapterError {

    }

    /**
     * placement is null or empty
     */
    public static final int ERROR_INVALID_PLACEMENT = 101;

    public static final int ERROR_SHOW_FAIL = 102;

    public static final int ERROR_BANNER_AD_SIZE_IS_NULL = 103;


    @NonNull
    public static AdError createAdapterError(@AdapterError int error, @NonNull String errorMessage) {
        return new AdError(error, errorMessage, ERROR_DOMAIN);
    }

    @NonNull
    public static AdError createSdkError(int error, @NonNull String errorMessage) {
        return new AdError(error, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
    }
}
