// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.chartboost;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.StartError;
import com.google.android.gms.ads.AdError;
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

  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link CacheError}.
   *
   * @param cacheError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull CacheError cacheError) {
    return new AdError(cacheError.getCode().getErrorCode(), cacheError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link ShowError}.
   *
   * @param showError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull ShowError showError) {
    return new AdError(showError.getCode().getErrorCode(), showError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link ClickError}.
   *
   * @param clickError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull ClickError clickError) {
    return new AdError(clickError.getCode().getErrorCode(), clickError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }

  /**
   * Creates an {@link AdError} object given Chartboost's {@link StartError}.
   *
   * @param startError Chartboost's error.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull StartError startError) {
    return new AdError(startError.getCode().getErrorCode(), startError.toString(),
        CHARTBOOST_SDK_ERROR_DOMAIN);
  }
}
