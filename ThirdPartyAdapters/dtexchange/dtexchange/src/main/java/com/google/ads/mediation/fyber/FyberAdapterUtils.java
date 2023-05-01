// Copyright 2020 Google LLC
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

package com.google.ads.mediation.fyber;

import static com.google.ads.mediation.fyber.FyberMediationAdapter.ERROR_DOMAIN;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveUserConfig;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus;
import com.google.android.gms.ads.AdError;

/**
 * Utility class for the DT Exchange adapter.
 */
class FyberAdapterUtils {

  /**
   * Private constructor
   */
  private FyberAdapterUtils() {
  }

  /**
   * Gets the specific error code for the specified {@link FyberInitStatus}.
   *
   * @param initStatus the initialization state from DT Exchange.
   * @return specific error.
   */
  static AdError getAdError(@NonNull FyberInitStatus initStatus) {
    // Error '299' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 299;
    switch (initStatus) {
      case SUCCESSFULLY:
        code = 200;
        break;
      case FAILED_NO_KITS_DETECTED:
        code = 201;
        break;
      case FAILED:
        code = 202;
        break;
      case INVALID_APP_ID:
        code = 203;
        break;
    }
    return new AdError(
        code, "DT Exchange failed to initialize with reason: " + initStatus, ERROR_DOMAIN);
  }

  /**
   * Gets the specific error code for the specified {@link InneractiveErrorCode}.
   *
   * @param inneractiveErrorCode the inneractive error code for ad request fail reason.
   * @return specific error.
   */

  static AdError getAdError(@NonNull InneractiveErrorCode inneractiveErrorCode) {
    // Error '399' to indicate that the error is new and has not been supported by the adapter yet.
    int code = 399;
    switch (inneractiveErrorCode) {
      case CONNECTION_ERROR:
        code = 300;
        break;
      case CONNECTION_TIMEOUT:
        code = 301;
        break;
      case NO_FILL:
        code = 302;
        break;
      case SERVER_INVALID_RESPONSE:
        code = 303;
        break;
      case SERVER_INTERNAL_ERROR:
        code = 304;
        break;
      case SDK_INTERNAL_ERROR:
        code = 305;
        break;
      case UNSPECIFIED:
        code = 306;
        break;
      case LOAD_TIMEOUT:
        code = 307;
        break;
      case INVALID_INPUT:
        code = 308;
        break;
      case SPOT_DISABLED:
        code = 309;
        break;
      case UNSUPPORTED_SPOT:
        code = 310;
        break;
      case IN_FLIGHT_TIMEOUT:
        code = 311;
        break;
      case SDK_NOT_INITIALIZED:
        code = 312;
        break;
      case NON_SECURE_CONTENT_DETECTED:
        code = 313;
        break;
      case ERROR_CONFIGURATION_MISMATCH:
        code = 314;
        break;
      case NATIVE_ADS_NOT_SUPPORTED_FOR_OS:
        code = 315;
        break;
      case ERROR_CONFIGURATION_NO_SUCH_SPOT:
        code = 316;
        break;
      case SDK_NOT_INITIALIZED_OR_CONFIG_ERROR:
        code = 317;
        break;
      case ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED:
        break;
    }
    return new AdError(
        code,
        "DT Exchange failed to request ad with reason: " + inneractiveErrorCode,
        ERROR_DOMAIN);
  }

  /**
   * Extract age from mediation extras and add it to DT Exchange SDK's global user params setting.
   *
   * @param mediationExtras mediation extras bundle
   */
  static void updateFyberExtraParams(@Nullable Bundle mediationExtras) {
    if (mediationExtras == null) {
      return;
    }

    InneractiveUserConfig userParams = new InneractiveUserConfig();
    if (mediationExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
      int age = mediationExtras.getInt(InneractiveMediationDefs.KEY_AGE, 0);
      userParams.setAge(age);
    }
    InneractiveAdManager.setUserParams(userParams);

    if (mediationExtras.containsKey(FyberMediationAdapter.KEY_MUTE_VIDEO)) {
      boolean muteState = mediationExtras.getBoolean(FyberMediationAdapter.KEY_MUTE_VIDEO, false);
      InneractiveAdManager.setMuteVideo(muteState);
    }
  }
}
