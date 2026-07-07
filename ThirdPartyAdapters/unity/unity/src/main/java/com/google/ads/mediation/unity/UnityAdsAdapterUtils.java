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
import static com.google.ads.mediation.unity.UnityMediationAdapter.TAG;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.unity3d.ads.BannerSize;
import com.unity3d.ads.MediationInfo;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;
import com.unity3d.ads.metadata.MetaData;
import java.util.ArrayList;
import java.util.Arrays;

/** Utility class for the Unity adapter. */
public class UnityAdsAdapterUtils {

  static final String ADMOB = "AdMob";

  static final String WATERMARK = "watermark";

  /** Enumeration of ad events that get forwarded to AdMob */
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

  /** Represents the result of a consent check for advertising purposes. */
  public enum ConsentResult {
    /** The consent status could not be determined, or consent does not apply. */
    UNKNOWN,
    /** The user has given their consent. */
    TRUE,
    /** The user has explicitly declined consent. */
    FALSE
  }

  /** Private constructor */
  private UnityAdsAdapterUtils() {}

  /**
   * Creates an {@link AdError} object based on the specified Initialization {@link
   * UnityAdsError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKInitializationError(
          @NonNull UnityAdsError unityAdsError, @NonNull String description) {
    return createAdError(getMediationInitializationErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified load {@link
   * UnityAdsError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKLoadError(
          @NonNull UnityAdsError unityAdsError, @NonNull String description) {
    return createAdError(getMediationLoadErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified show {@link
   * UnityAdsError}.
   *
   * @param unityAdsError error object from Unity.
   * @param description the error message.
   * @return the {@link AdError} object.
   */
  @NonNull
  static AdError createSDKShowError(
          @NonNull UnityAdsError unityAdsError, @NonNull String description) {
    return createAdError(getMediationShowErrorCode(unityAdsError), description);
  }

  /**
   * Creates an {@link AdError} object based on the specified error code and description
   *
   * @param errorCode the mediation error code.
   * @param description the error message.
   * @return the error.
   */
  @NonNull
  static AdError createAdError(int errorCode, @NonNull String description) {
    return new AdError(errorCode, description, SDK_ERROR_DOMAIN);
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific load error code.
   */
  static int getMediationInitializationErrorCode(@NonNull UnityAdsError unityAdsError) {
    switch (unityAdsError.getCode()) {
      case 2:
      case 52000:
      case 52003:
      case 52004:
      case 52005:
      case 52006: // Internal Error
        return 301;
      case 52001:
      case 52002: // Invalid argument
        return 302;

      // Excluding default to allow for compile warnings if UnityAdsInitializationError is
      // expanded in the future.
    }
    return 300;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific load error code.
   */
  static int getMediationLoadErrorCode(@NonNull UnityAdsError unityAdsError) {
    switch (unityAdsError.getCode()) {
      case 52101: // Not initialized
        return 401;
      case 52102:
      case 52104: // Invalid arguments
        return 403;
      case 52100: // No fill
        return 404;
      case 2: // Time out
        return 405;
      case 52103:
      case 52105:
      case 52106:
      case 52107:// Internal error
        return 402;
      // Excluding default to allow for compile warnings if UnityAdsLoadError is expanded
      // in the future.
    }

    return 400;
  }

  /**
   * Gets the mediation specific error code for the specified {@link UnityAds.UnityAdsLoadError}.
   *
   * @param unityAdsError error object from Unity.
   * @return mediation specific load error code.
   */
  static int getMediationShowErrorCode(@NonNull UnityAdsError unityAdsError) {
    switch (unityAdsError.getCode()) {
      case 52201: // already showing
        return 506;
      case 52200: // Expired
      case 52202: // internal error:
        return 507;
      case 2: // timeout:
        return 508;

      // Excluding default to allow for compile warnings if UnityAdsShowError is expanded
      // in the future.
    }
    return 500;
  }

  @Nullable
  public static BannerSize getUnityBannerSize(
      @NonNull Context context,
      @NonNull AdSize adSize,
      boolean isRtb,
      MediationUtilsWrapper mediationUtils) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);

    AdSize closestSize = mediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize != null) {
      return new BannerSize(closestSize.getWidth(), closestSize.getHeight());
    }

    if (isRtb) {
      return new BannerSize(adSize.getWidth(), adSize.getHeight());
    } else {
      return null;
    }
  }

  /**
   * Set the COPPA setting in Unity Ads SDK.
   *
   * @param requestConfiguration used to read the value that indicates whether the app should be
   *     treated as child-directed for purposes of the COPPA or under age consent.
   */
  public static void setUnityAdsPrivacy(RequestConfiguration requestConfiguration) {
    UnityAds.setNonBehavioral(!shouldTreatAsAdult(requestConfiguration));
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

  /**
   * Checks whether the user provided consent to a Google Ad Tech Provider (ATP) in Google’s
   * Additional Consent technical specification. For more details, see <a
   * href="https://support.google.com/admob/answer/9681920">Google’s Additional Consent technical
   * specification</a>.
   *
   * <p>Returns {@link ConsentResult#UNKNOWN} if GDPR does not apply or if positive or negative
   * consent was not explicitly detected.
   *
   * @param context {@link Context} object of your application
   * @param vendorId a Google Ad Tech Provider (ATP) ID from
   *     https://storage.googleapis.com/tcfac/additional-consent-providers.csv
   * @return A {@link ConsentResult} indicating consent for the given ATP.
   */
  static @NonNull ConsentResult hasACConsent(@NonNull Context context, int vendorId) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

    int gdprApplies = -1;
    try {
      gdprApplies = sharedPref.getInt("IABTCF_gdprApplies", -1);
    } catch (ClassCastException exception) {
      Log.w(
          TAG,
          "Could not parse IABTCF_gdprApplies as an integer. Did your CMP write it correctly?",
          exception);
    }

    if (gdprApplies != 1) {
      return ConsentResult.UNKNOWN;
    }

    String additionalConsentString = "";
    try {
      additionalConsentString = sharedPref.getString("IABTCF_AddtlConsent", "");
    } catch (ClassCastException exception) {
      Log.w(
          TAG,
          "Could not parse IABTCF_AddtlConsent as a string. Did your CMP write it correctly?",
          exception);
    }

    if (TextUtils.isEmpty(additionalConsentString)) {
      return ConsentResult.UNKNOWN;
    }

    String vendorIdString = String.valueOf(vendorId);
    String[] additionalConsentParts = additionalConsentString.split("~");

    int version;
    try {
      version = Integer.parseInt(additionalConsentParts[0]);
    } catch (Exception exception) {
      Log.w(
          TAG,
          "Could not parse the IABTCF_AddtlConsent spec version. Did your CMP write it correctly?",
          exception);
      return ConsentResult.UNKNOWN;
    }

    if (version == 1) {
      // Spec version 1
      Log.w(
          TAG,
          "The IABTCF_AddtlConsent string uses version 1 of Google’s Additional Consent spec."
              + " Version 1 does not report vendors to whom the user denied consent. To detect"
              + " vendors that the user denied consent, upgrade to a CMP that supports version 2 of"
              + " Google's Additional Consent technical specification.");

      if (additionalConsentParts.length == 1) {
        // The AC string had no consented vendor.
        return ConsentResult.UNKNOWN;
      }

      if (additionalConsentParts.length == 2) {
        String[] consentedIds = additionalConsentParts[1].split("\\.");
        if (Arrays.asList(consentedIds).contains(vendorIdString)) {
          return ConsentResult.TRUE;
        }

        // The targeting vendor ID is not included in the consented vendor list
        return ConsentResult.UNKNOWN;
      }

      String errorMessage =
          String.format(
              "Could not parse the IABTCF_AddtlConsent string: \"%s\". String had more parts than"
                  + " expected. Did your CMP write IABTCF_AddtlConsent correctly?",
              additionalConsentString);
      Log.w(TAG, errorMessage);
      return ConsentResult.UNKNOWN;
    } else if (version >= 2) {
      // Spec version 2
      if (additionalConsentParts.length < 3) {
        String errorMessage =
            String.format(
                "Could not parse the IABTCF_AddtlConsent string: \"%s\". String had less parts than"
                    + " expected. Did your CMP write IABTCF_AddtlConsent correctly?",
                additionalConsentString);
        Log.w(TAG, errorMessage);
        return ConsentResult.UNKNOWN;
      }

      String[] disclosedIds = additionalConsentParts[2].split("\\.");
      if (!disclosedIds[0].equals("dv")) {
        String errorMessage =
            String.format(
                "Could not parse the IABTCF_AddtlConsent string: \"%s\". Expected disclosed vendors"
                    + " part to have the string \"dv.\". Did your CMP write IABTCF_AddtlConsent"
                    + " correctly?",
                additionalConsentString);
        Log.w(TAG, errorMessage);
        return ConsentResult.UNKNOWN;
      }

      String[] consentedIds = additionalConsentParts[1].split("\\.");
      if (Arrays.asList(consentedIds).contains(vendorIdString)) {
        return ConsentResult.TRUE;
      }

      if (Arrays.asList(disclosedIds).contains(vendorIdString)) {
        return ConsentResult.FALSE;
      }

      return ConsentResult.UNKNOWN;
    } else {
      // Unknown spec version
      String errorMessage =
          String.format(
              "Could not parse the IABTCF_AddtlConsent string: \"%s\". Spec version was unexpected."
                  + " Did your CMP write IABTCF_AddtlConsent correctly?",
              additionalConsentString);
      Log.w(TAG, errorMessage);
      return ConsentResult.UNKNOWN;
    }
  }

  /**
   * Returns true if none TFCD nor TFUA are True and at least one of them is
   *
   * @param requestConfiguration used to read both signals tag for Child Treatment and tag for Under
   *     Age Consent
   * @return if session will be treat the user as adult by UnityAds.
   */
  private static boolean shouldTreatAsAdult(RequestConfiguration requestConfiguration) {
    int tagForChildDirectedTreatment = requestConfiguration.getTagForChildDirectedTreatment();
    int tagForUnderAgeOfConsent = requestConfiguration.getTagForUnderAgeOfConsent();

    return tagForChildDirectedTreatment != TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        && tagForUnderAgeOfConsent != TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        && (tagForChildDirectedTreatment == TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
            || tagForUnderAgeOfConsent == TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE);
  }

  /**
   * Creates a MediationInfo object with Google mediation information.
   * This duplicates the logic from UnityAdsWrapper for convenience.
   *
   * @return MediationInfo object with Google mediation details
   */
  public static MediationInfo getMediationInfo() {
    return new MediationInfo(
            ADMOB,                              // Mediation name
            MobileAds.getVersion().toString(),  // Google mediation SDK version
            BuildConfig.ADAPTER_VERSION         // Adapter version
    );
  }
}
