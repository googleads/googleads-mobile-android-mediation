// Copyright 2018 Google LLC
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

package com.applovin.mediation;

import static com.google.ads.mediation.applovin.AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import java.util.ArrayList;

/**
 * A helper class used by {@link ApplovinAdapter}.
 */
public class AppLovinUtils {

  private static final String DEFAULT_ZONE = "";

  @VisibleForTesting
  public static final String ERROR_MSG_REASON_PREFIX =
      "AppLovin SDK returned a load failure callback with reason: ";

  /**
   * Keys for retrieving values from the server parameters.
   */
  public static class ServerParameterKeys {

    public static final String SDK_KEY = "sdkKey";
    public static final String ZONE_ID = "zone_id";

    // Private constructor
    private ServerParameterKeys() {
    }
  }

  /**
   * Retrieves the zone identifier from an appropriate connector object. Will use empty string if
   * none exists.
   */
  public static String retrieveZoneId(Bundle serverParameters) {
    if (serverParameters.containsKey(ServerParameterKeys.ZONE_ID)) {
      return serverParameters.getString(ServerParameterKeys.ZONE_ID);
    } else {
      return DEFAULT_ZONE;
    }
  }

  /**
   * Retrieves whether or not to mute the ad that is about to be rendered.
   */
  public static boolean shouldMuteAudio(Bundle networkExtras) {
    return networkExtras != null && networkExtras.getBoolean(AppLovinExtras.Keys.MUTE_AUDIO);
  }

  /**
   * Convert the given AppLovin SDK error code into a Google AdError.
   */
  public static AdError getAdError(int applovinErrorCode) {
    String reason = "AppLovin error code " + applovinErrorCode;
    switch (applovinErrorCode) {
      case AppLovinErrorCodes.NO_FILL:
        reason = "NO_FILL";
        break;
      case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
        reason = "FETCH_AD_TIMEOUT";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED:
        reason = "INCENTIVIZED_NO_AD_PRELOADED";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT:
        reason = "INCENTIVIZED_SERVER_TIMEOUT";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR:
        reason = "INCENTIVIZED_UNKNOWN_SERVER_ERROR";
        break;
      case AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO:
        reason = "INCENTIVIZED_USER_CLOSED_VIDEO";
        break;
      case AppLovinErrorCodes.INVALID_AD_TOKEN:
        reason = "INVALID_AD_TOKEN";
        break;
      case AppLovinErrorCodes.INVALID_RESPONSE:
        reason = "INVALID_RESPONSE";
        break;
      case AppLovinErrorCodes.INVALID_URL:
        reason = "INVALID_URL";
        break;
      case AppLovinErrorCodes.INVALID_ZONE:
        reason = "INVALID_ZONE";
        break;
      case AppLovinErrorCodes.NO_NETWORK:
        reason = "NO_NETWORK";
        break;
      case AppLovinErrorCodes.SDK_DISABLED:
        reason = "SDK_DISABLED";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_IMAGE_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_IMAGE_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_PRECACHE_VIDEO_RESOURCES:
        reason = "UNABLE_TO_PRECACHE_VIDEO_RESOURCES";
        break;
      case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
        reason = "UNABLE_TO_RENDER_AD";
        break;
      case AppLovinErrorCodes.UNSPECIFIED_ERROR:
        reason = "UNSPECIFIED_ERROR";
        break;
      default: // fall out
    }

    return new AdError(
        applovinErrorCode, ERROR_MSG_REASON_PREFIX + reason, APPLOVIN_SDK_ERROR_DOMAIN);
  }

  /**
   * Get the {@link AppLovinAdSize} from a given {@link AdSize} from AdMob.
   */
  @Nullable
  public static AppLovinAdSize appLovinAdSizeFromAdMobAdSize(@NonNull Context context,
      @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);
    potentials.add(AdSize.MEDIUM_RECTANGLE);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (AdSize.BANNER.equals(closestSize)) {
      return AppLovinAdSize.BANNER;
    } else if (AdSize.MEDIUM_RECTANGLE.equals(closestSize)) {
      return AppLovinAdSize.MREC;
    } else if (AdSize.LEADERBOARD.equals(closestSize)) {
      return AppLovinAdSize.LEADER;
    }
    return null;
  }
}
