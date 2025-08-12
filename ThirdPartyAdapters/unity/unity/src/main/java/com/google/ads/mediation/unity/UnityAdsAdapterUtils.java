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

package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityMediationAdapter.SDK_ERROR_DOMAIN;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAds.UnityAdsInitializationError;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.UnityBannerSize;
import java.util.ArrayList;

/**
 * Utility class for the Unity adapter.
 */
public class UnityAdsAdapterUtils {

  /**
   * Enumeration of ad events that get forwarded to AdMob
   */
  public enum AdEvent {
    LOADED,
    OPENED,
    CLICKED,
    CLOSED,
    LEFT_APPLICATION,
    IMPRESSION,
    VIDEO_START,
    REWARD,
    VIDEO_COMPLETE
  }

  /**
   * Private constructor
   */
  private UnityAdsAdapterUtils() {
  }

  /**
   * Creates an {@link AdError} object based on the specified
   * {@link UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsInitializationError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsLoadError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified {@link UnityAds.UnityAdsShowError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description   the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKError(@NonNull UnityAds.UnityAdsShowError unityAdsError,
      @NonNull String description) {
    return createAdError(getMediationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified error code and description
   *
   * @param errorCode   the mediation error code.
   * @param description the error message.
   * @return the error.
   */
  @NonNull
  static AdError createAdError(int errorCode, @NonNull String description) {
    return new AdError(errorCode, description, SDK_ERROR_DOMAIN);
  }

  /**
   * Gets the mediation specific error code for the specified {@link BannerErrorInfo}.
   *
   * @param errorInfo error object from Unity.
   * @return mediation specific banner error code.
   */
  static int getMediationErrorCode(@NonNull BannerErrorInfo errorInfo) {
    int errorCode = 200;
    switch (errorInfo.errorCode) {
      case UNKNOWN:
        errorCode = 201;
        break;
      case NATIVE_ERROR:
        errorCode = 202;
        break;
      case WEBVIEW_ERROR:
        errorCode = 203;
        break;
      case NO_FILL:
        errorCode = 204;
        break;
    }
    return errorCode;
  }

  /**
   * Gets the mediation specific error code for the specified
   * {@link UnityAds.UnityAdsInitializationError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific initialization error code.
   */
  static int getMediationErrorCode(@NonNull UnityAdsInitializationError unityAdsError) {
    switch (unityAdsError) {
      case INTERNAL_ERROR:
        return 301;
      case INVALID_ARGUMENT:
        return 302;
      case AD_BLOCKER_DETECTED:
        return 303;
      // Excluding default to allow for compile warnings if UnityAdsInitializationError is expanded
      // in the future.
    }
    return 300;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific load error code.
   */
  static int getMediationErrorCode(@NonNull UnityAds.UnityAdsLoadError unityAdsError) {
    switch (unityAdsError) {
      case INITIALIZE_FAILED:
        return 401;
      case INTERNAL_ERROR:
        return 402;
      case INVALID_ARGUMENT:
        return 403;
      case NO_FILL:
        return 404;
      case TIMEOUT:
        return 405;
      // Excluding default to allow for compile warnings if UnityAdsLoadError is expanded
      // in the future.
    }
    return 400;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsShowError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific show error code.
   */
  static int getMediationErrorCode(@NonNull UnityAds.UnityAdsShowError unityAdsError) {
    switch (unityAdsError) {
      case NOT_INITIALIZED:
        return 501;
      case NOT_READY:
        return 502;
      case VIDEO_PLAYER_ERROR:
        return 503;
      case INVALID_ARGUMENT:
        return 504;
      case NO_CONNECTION:
        return 505;
      case ALREADY_SHOWING:
        return 506;
      case INTERNAL_ERROR:
        return 507;
      // Excluding default to allow for compile warnings if UnityAdsShowError is expanded
      // in the future.
    }
    return 500;
  }

  @Nullable
  public static UnityBannerSize getUnityBannerSize(
      @NonNull Context context, @NonNull AdSize adSize, boolean isRtb) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize != null) {
      return new UnityBannerSize(closestSize.getWidth(), closestSize.getHeight());
    }

    if (isRtb) {
      return new UnityBannerSize(adSize.getWidth(), adSize.getHeight());
    } else {
      return null;
    }
  }

  /**
   * Set the COPPA setting in Unity Ads SDK.
   *
   * @param coppa an {@code Integer} value that indicates whether the app should be treated as
   *     child-directed for purposes of the COPPA. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE} means true. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE} means false. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED} means unspecified.
   */
  public static void setCoppa(@TagForChildDirectedTreatment int coppa, @NonNull Context context) {

    MetaData userMetaData = new MetaData(context);
    if (coppa == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
      userMetaData.set("user.nonbehavioral", false);
    } else {
      // Unity Ads will default to treating users as children when a user-level COPPA designation is
      // absent.
      userMetaData.set("user.nonbehavioral", true);
    }
    userMetaData.commit();
  }

  /**
   * Checks whether or not the provided Unity Ads IDs are valid.
   *
   * @param gameId Unity Ads Game ID to be verified.
   * @param placementId Unity Ads Placement ID to be verified.
   * @return {@code true} if all the IDs provided are valid.
   */
  public static boolean areValidIds(String gameId, String placementId) {
    return !TextUtils.isEmpty(gameId) && !TextUtils.isEmpty(placementId);
  }

  /** Gets the ad format from RtbSignalData. */
  @Nullable
  static AdFormat getAdFormat(RtbSignalData rtbSignalData) {
    for (MediationConfiguration mediationConfiguration : rtbSignalData.getConfigurations()) {
      return mediationConfiguration.getFormat();
    }
    return null;
  }
}