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
                    ERROR_SHOW_AD_NOT_LOADED,
                    ERROR_BANNER_AD_SIZE_IS_INVALID,
            })
    public @interface AdapterError {

    }

    /**
     * placement is null or empty
     */
    public static final int ERROR_INVALID_PLACEMENT = 101;

    /**
     * Ads are not loaded[RewardAd、InterstitialAd]
     */
    public static final int ERROR_SHOW_AD_NOT_LOADED = 102;

    /**
     * The ad size could not be obtained, or the ad size is illegal
     */
    public static final int ERROR_BANNER_AD_SIZE_IS_INVALID = 103;

    /**
     * The returned ad object is null
     */
    public static final int ERROR_AD_NOT_FILL = 104;

    @NonNull
    public static AdError createAdapterError(@AdapterError int error, @NonNull String errorMessage) {
        return new AdError(error, errorMessage, ERROR_DOMAIN);
    }

    @NonNull
    public static AdError createSdkError(int error, @NonNull String errorMessage) {
        return new AdError(error, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
    }
}
