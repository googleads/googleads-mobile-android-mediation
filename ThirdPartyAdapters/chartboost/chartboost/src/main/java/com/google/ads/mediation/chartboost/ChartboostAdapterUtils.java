// Copyright 2019 Google LLC
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

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.privacy.model.CCPA.CCPA_CONSENT;
import com.chartboost.sdk.privacy.model.COPPA;
import com.google.ads.mediation.common.AgeRestrictedTreatmentUtils;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AgeRestrictedTreatment;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.RequestConfiguration;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility methods for the Chartboost Adapter.
 */
class ChartboostAdapterUtils {

  /** Represents the result of a consent check for advertising purposes. */
  public enum ConsentResult {
    /** The consent status could not be determined, or consent does not apply. */
    UNKNOWN,
    /** The user has given their consent. */
    TRUE,
    /** The user has explicitly declined consent. */
    FALSE
  }

  /**
   * Key to obtain App ID, required for initializing Chartboost SDK.
   */
  static final String KEY_APP_ID = "appId";

  /**
   * Key to obtain App Signature, required for initializing Charboost SDK.
   */
  static final String KEY_APP_SIGNATURE = "appSignature";

  /**
   * Key to obtain Ad Location. This is added in adapter version 1.1.0.
   */
  static final String KEY_AD_LOCATION = "adLocation";

  /**
   * Default location for Chartboost ads. Chartboost aggregates reporting for ads with no named
   * location under "Default", so this must match that name exactly.
   */
  static final String LOCATION_DEFAULT = "Default";

  /**
   * Key used by IAB-compliant consent management platforms (CMPs) to write the US Privacy String
   * to {@link android.content.SharedPreferences}.
   */
  static final String KEY_US_PRIVACY_STRING = "IABUSPrivacy_String";

  /** Expected length of a well formed US Privacy String. */
  private static final int US_PRIVACY_STRING_LENGTH = 4;

  /** Index of the spec version character within the US Privacy String. */
  private static final int US_PRIVACY_STRING_INDEX_VERSION = 0;

  /** Index of the opt-out-of-sale character within the US Privacy String. */
  private static final int US_PRIVACY_STRING_INDEX_OPT_OUT_SALE = 2;

  /** The only spec version of the US Privacy String that this adapter understands. */
  private static final char US_PRIVACY_STRING_VERSION_1 = '1';

  /** Opt-out-of-sale character indicating the user opted out of the sale of their data. */
  private static final char US_PRIVACY_OPT_OUT_SALE_YES = 'Y';

  /** Opt-out-of-sale character indicating the user did not opt out of the sale of their data. */
  private static final char US_PRIVACY_OPT_OUT_SALE_NO = 'N';

  /** Opt-out-of-sale character indicating that opt-out of sale does not apply to the user. */
  private static final char US_PRIVACY_OPT_OUT_SALE_NOT_APPLICABLE = '-';

  /**
   * Chartboost mediation object.
   */
  private static Mediation chartboostMediation;

  /**
   * Creates and return a new {@link ChartboostParams} object populated with the parameters obtained
   * from the server parameters and network extras bundles.
   *
   * @param serverParameters a {@link Bundle} containing server parameters used to initialize
   *                         Chartboost.
   * @return a {@link ChartboostParams} object populated with the params obtained from the bundles
   * provided.
   */
  static ChartboostParams createChartboostParams(@NonNull Bundle serverParameters) {
    ChartboostParams chartboostParams = new ChartboostParams();
    String appId = serverParameters.getString(KEY_APP_ID);
    String appSignature = serverParameters.getString(KEY_APP_SIGNATURE);
    if (appId != null && appSignature != null) {
      chartboostParams.setAppId(appId.trim());
      chartboostParams.setAppSignature(appSignature.trim());
    }

    String adLocation = serverParameters.getString(KEY_AD_LOCATION);
    if (TextUtils.isEmpty(adLocation)) {
      // Ad Location is empty, log a warning and use the default location.
      String logMessage =
          String.format(
              "Chartboost ad location is empty, defaulting to %s. "
                  + "Please set the Ad Location parameter in the AdMob UI.",
              LOCATION_DEFAULT);
      Log.w(TAG, logMessage);
      adLocation = LOCATION_DEFAULT;
    }
    chartboostParams.setLocation(adLocation.trim());
    return chartboostParams;
  }

  /**
   * Checks whether or not the provided {@link ChartboostParams} is valid.
   *
   * @param chartboostParams Chartboost params to be examined.
   * @return {@code true} if the given ChartboostParams' appId and appSignature are valid, false
   * otherwise.
   */
  static boolean isValidChartboostParams(@Nullable ChartboostParams chartboostParams) {
    if (chartboostParams == null) {
      return false;
    }

    if (TextUtils.isEmpty(chartboostParams.getAppId()) || TextUtils.isEmpty(
        chartboostParams.getAppSignature())) {
      Log.e(TAG,
          "Missing or invalid App ID or App Signature configured for this ad source instance"
              + "in the AdMob or Ad Manager UI.");
      return false;
    }

    return true;
  }

  /**
   * Returns the closest possible {@link Banner.BannerSize} format based on the provided {@link
   * AdSize}.
   *
   * @param context the context of requesting banner ad.
   * @param adSize  the requested banner ad size.
   * @return Chartboost {@link Banner.BannerSize} object.
   */
  @Nullable
  static Banner.BannerSize findClosestBannerSize(@NonNull Context context, @NonNull AdSize adSize) {
    AdSize standardSize =
        new AdSize(
            Banner.BannerSize.STANDARD.getWidth(), Banner.BannerSize.STANDARD.getHeight());
    AdSize mediumSize =
        new AdSize(Banner.BannerSize.MEDIUM.getWidth(), Banner.BannerSize.MEDIUM.getHeight());
    AdSize leaderboardSize =
        new AdSize(
            Banner.BannerSize.LEADERBOARD.getWidth(),
            Banner.BannerSize.LEADERBOARD.getHeight());
    AdSize halfPageSize =
        new AdSize(
            Banner.BannerSize.HALFPAGE.getWidth(), Banner.BannerSize.HALFPAGE.getHeight());

    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(standardSize);
    potentials.add(mediumSize);
    potentials.add(leaderboardSize);
    potentials.add(halfPageSize);

    AdSize supportedAdSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (supportedAdSize == null) {
      return null;
    }

    if (supportedAdSize.equals(standardSize)) {
      return Banner.BannerSize.STANDARD;
    } else if (supportedAdSize.equals(mediumSize)) {
      return Banner.BannerSize.MEDIUM;
    } else if (supportedAdSize.equals(leaderboardSize)) {
      return Banner.BannerSize.LEADERBOARD;
    } else if (supportedAdSize.equals(halfPageSize)) {
      return Banner.BannerSize.HALFPAGE;
    }
    return null;
  }

  /**
   * Returns a {@link Mediation} object that contains mediation information. This will be
   * called every time a Chartboost ad object is created.
   */
  static Mediation getChartboostMediation() {
    if (chartboostMediation == null) {
      chartboostMediation = new Mediation("AdMob", Chartboost.getSDKVersion(),
          BuildConfig.ADAPTER_VERSION);
    }
    return chartboostMediation;
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
   * Reads the IAB US Privacy String written by the app's consent management platform (CMP) and
   * maps it to a Chartboost {@link CCPA_CONSENT} value.
   *
   * <p>The US Privacy String is a 4 character string stored under the key {@code
   * IABUSPrivacy_String}:
   *
   * <ul>
   *   <li>index 0 - the spec version, expected to be {@code '1'}.
   *   <li>index 1 - whether explicit notice was given.
   *   <li>index 2 - whether the user opted out of the sale of their data. This is the only
   *       character this method uses.
   *   <li>index 3 - whether a limited service provider agreement applies.
   * </ul>
   *
   * <p>Returns {@code null} (no consent signal, so the Chartboost SDK is not called) when the
   * string is absent, empty, a different length than expected, on an unsupported spec version,
   * or when the opt-out-of-sale character is {@code '-'} (not applicable) or any other
   * unrecognized character.
   *
   * @param context {@link Context} object of your application.
   * @return {@link CCPA_CONSENT#OPT_OUT_SALE} if the user opted out of the sale of their data,
   *     {@link CCPA_CONSENT#OPT_IN_SALE} if the user did not, or {@code null} if no signal could
   *     be determined.
   * @see <a
   *     href="https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/US%20Privacy%20String.md">IAB
   *     US Privacy String spec</a>
   */
  static @Nullable CCPA_CONSENT readUsPrivacyConsent(@NonNull Context context) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

    String usPrivacyString = "";
    try {
      usPrivacyString = sharedPref.getString(KEY_US_PRIVACY_STRING, "");
    } catch (ClassCastException exception) {
      Log.w(
          TAG,
          "Could not parse IABUSPrivacy_String as a string. Did your CMP write it correctly?",
          exception);
    }

    if (TextUtils.isEmpty(usPrivacyString)) {
      return null;
    }

    if (usPrivacyString.length() != US_PRIVACY_STRING_LENGTH) {
      Log.w(
          TAG,
          "Ignoring an IABUSPrivacy_String of length "
              + usPrivacyString.length()
              + ". Expected "
              + US_PRIVACY_STRING_LENGTH
              + " characters. Did your CMP write it correctly?");
      return null;
    }

    if (usPrivacyString.charAt(US_PRIVACY_STRING_INDEX_VERSION) != US_PRIVACY_STRING_VERSION_1) {
      Log.w(
          TAG,
          "Ignoring an IABUSPrivacy_String on unsupported spec version "
              + usPrivacyString.charAt(US_PRIVACY_STRING_INDEX_VERSION)
              + ". This adapter only understands version "
              + US_PRIVACY_STRING_VERSION_1
              + ".");
      return null;
    }

    char optOutOfSale = usPrivacyString.charAt(US_PRIVACY_STRING_INDEX_OPT_OUT_SALE);
    if (optOutOfSale == US_PRIVACY_OPT_OUT_SALE_YES) {
      return CCPA_CONSENT.OPT_OUT_SALE;
    }

    if (optOutOfSale == US_PRIVACY_OPT_OUT_SALE_NO) {
      return CCPA_CONSENT.OPT_IN_SALE;
    }

    // optOutOfSale is either US_PRIVACY_OPT_OUT_SALE_NOT_APPLICABLE or an unrecognized
    // character; either way there is no consent signal to report.
    return null;
  }

  static void updateCoppaStatus(
      Context context, @NonNull RequestConfiguration requestConfiguration) {
    // Chartboost's SDK only supports updating a user's COPPA status with true and false values.
    int tagForChildDirected = requestConfiguration.getTagForChildDirectedTreatment();
    int tagForUnderAgeConsent = requestConfiguration.getTagForUnderAgeOfConsent();
    boolean isAgeRestrictedTreatmentChild =
        AgeRestrictedTreatmentUtils.runtimeGmaSdkSupportsChildAgeRestrictedTreatment()
            && requestConfiguration.getAgeRestrictedTreatment() == AgeRestrictedTreatment.CHILD;
    if (tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
        || tagForUnderAgeConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
        || isAgeRestrictedTreatmentChild) {
        Chartboost.addDataUseConsent(context, new COPPA(true));
    } else if (tagForChildDirected == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        || tagForUnderAgeConsent == RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE) {
      Chartboost.addDataUseConsent(context, new COPPA(false));
    }
  }

  static String getAdapterVersion() {
    return BuildConfig.ADAPTER_VERSION;
  }
}
