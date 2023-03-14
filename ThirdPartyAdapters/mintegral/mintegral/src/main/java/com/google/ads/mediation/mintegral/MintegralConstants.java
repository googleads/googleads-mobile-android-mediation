package com.google.ads.mediation.mintegral;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MintegralConstants {

  public static final String APP_ID = "app_id";
  public static final String APP_KEY = "app_key";
  public static final String AD_UNIT_ID = "ad_unit_id";
  public static final String PLACEMENT_ID = "placement_id";
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mintegral";
  public static final String MINTEGRAL_SDK_ERROR_DOMAIN = "com.mbridge.msdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {ERROR_INVALID_SERVER_PARAMETERS, ERROR_BANNER_SIZE_UNSUPPORTED,
      ERROR_INVALID_BID_RESPONSE, ERROR_MINTEGRAL_SDK, ERROR_CODE_NO_FILL,})
  public @interface AdapterError {

  }

  /**
   * The Mintegral SDK returned a failure callback.
   */
  public static final int ERROR_MINTEGRAL_SDK = 100;

  /**
   * Invalid server parameters (e.g. Missing App ID or Placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Mintegral supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_UNSUPPORTED = 102;

  /**
   * Missing or invalid bid response.
   */
  public static final int ERROR_INVALID_BID_RESPONSE = 103;

  /**
   * Mintegral SDK returned a no fill error.
   */
  public static final int ERROR_CODE_NO_FILL = 104;

  /**
   * The returned Mintegral SDK init failed;
   */
  public static final int ERROR_CODE_SDK_INIT_FAILED = 105;



  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(int errorCode,@NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, MINTEGRAL_SDK_ERROR_DOMAIN);
  }
}
