package com.google.ads.mediation.pangle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PangleConstants {

  public static final String PLACEMENT_ID = "placementid";
  public static final String APP_ID = "appid";
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.pangle";
  public static final String PANGLE_SDK_ERROR_DOMAIN = "com.pangle.ads";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_INVALID_BID_RESPONSE,
      })
  public @interface AdapterError {

  }

  /**
   * Invalid server parameters (e.g. Missing app ID or placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Pangle supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Missing or invalid bid response.
   */
  public static final int ERROR_INVALID_BID_RESPONSE = 103;

  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(int errorCode, @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, PANGLE_SDK_ERROR_DOMAIN);
  }
}
