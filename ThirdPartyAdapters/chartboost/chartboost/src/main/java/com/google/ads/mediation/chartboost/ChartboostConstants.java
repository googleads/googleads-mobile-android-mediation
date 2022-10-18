package com.google.ads.mediation.chartboost;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ChartboostConstants {

  // Chartboost adapter error domain.
  static final String ERROR_DOMAIN = "com.google.ads.mediation.chartboost";

  // Chartboost SDK error domain.
  static final String CHARTBOOST_SDK_ERROR_DOMAIN = "com.chartboost.sdk";

  /**
   * Chartboost adapter errors.
   */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {
          ERROR_BANNER_SIZE_MISMATCH,
          ERROR_AD_ALREADY_LOADED,
          ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_AD_NOT_READY
      })
  public @interface AdapterError {

  }

  /**
   * The requested ad size does not match a Chartboost supported banner size.
   */
  static final int ERROR_BANNER_SIZE_MISMATCH = 101;

  /**
   * Chartboost can only load 1 ad per location at a time.
   */
  static final int ERROR_AD_ALREADY_LOADED = 102;

  /**
   * Invalid server parameters (e.g. Chartboost App ID is missing).
   */
  static final int ERROR_INVALID_SERVER_PARAMETERS = 103;

  /**
   * Chartboost ad is not ready to be shown.
   */
  static final int ERROR_AD_NOT_READY = 104;
}
