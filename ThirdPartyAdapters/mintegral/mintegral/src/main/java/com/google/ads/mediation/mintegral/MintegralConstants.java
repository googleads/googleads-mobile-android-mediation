package com.google.ads.mediation.mintegral;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MintegralConstants {

  public static final String AD_UNIT_ID = "ad_unit_id";
  public static final String PLACEMENT_ID = "placement_id";
  public static final String APP_ID = "app_id";
  public static final String APP_KEY = "app_key";
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mintegral";
  public static final String MINTEGRAL_SDK_ERROR_DOMAIN = "com.mbridge.msdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {ERROR_MINTEGRAL_SDK,
      ERROR_INVALID_SERVER_PARAMETERS})
  public @interface AdapterError {

  }

  /**
   * The Mintegral SDK returned a failure callback.
   */
  public static final int ERROR_MINTEGRAL_SDK = 100;

  /**
   * Invalid server parameters (e.g Missing app ID or placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;


  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(@NonNull String errorMessage) {
    return new AdError(ERROR_MINTEGRAL_SDK, errorMessage, MINTEGRAL_SDK_ERROR_DOMAIN);
  }
}
