package com.google.ads.mediation.inmobi;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiNative;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.AgeGroup;
import com.inmobi.sdk.InMobiSdk.Education;
import com.inmobi.sdk.InMobiSdk.Gender;
import com.inmobi.sdk.InMobiSdk.LogLevel;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * This class contains the utility methods used by InMobi adapter.
 */
class InMobiAdapterUtils {

  static final String KEY_ACCOUNT_ID = "accountid";
  static final String KEY_PLACEMENT_ID = "placementid";

  static long getPlacementId(@NonNull Bundle serverParameters) {
    String placementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      Log.w(InMobiMediationAdapter.TAG, "Missing or Invalid Placement ID.");
      return 0L;
    }

    long placement = 0L;
    try {
      placement = Long.parseLong(placementId);
    } catch (NumberFormatException exception) {
      Log.w(InMobiMediationAdapter.TAG, "Invalid Placement ID.", exception);
    }
    return placement;
  }

  static void setGlobalTargeting(MediationRewardedAdConfiguration configuration, Bundle extras) {
    configureGlobalTargeting(extras);

    if (configuration.getLocation() != null) {
      InMobiSdk.setLocation(configuration.getLocation());
    }
  }

  static void setGlobalTargeting(MediationBannerAdConfiguration configuration, Bundle extras) {
    configureGlobalTargeting(extras);

    if (configuration.getLocation() != null) {
      InMobiSdk.setLocation(configuration.getLocation());
    }
  }

  static void setGlobalTargeting(MediationInterstitialAdConfiguration configuration, Bundle extras) {
    configureGlobalTargeting(extras);

    if (configuration.getLocation() != null) {
      InMobiSdk.setLocation(configuration.getLocation());
    }
  }

  static void setGlobalTargeting(MediationNativeAdConfiguration configuration, Bundle extras) {
    configureGlobalTargeting(extras);

    if (configuration.getLocation() != null) {
      InMobiSdk.setLocation(configuration.getLocation());
    }
  }

  private static void configureGlobalTargeting(Bundle extras) {
    if (extras == null) {
      Log.d(InMobiMediationAdapter.TAG, "Bundle extras are null");
      extras = new Bundle();
    }

    String city = "", state = "", country = "";

    Set<String> keySet = extras.keySet();
    for (String key : keySet) {

      String value = extras.getString(key);
      if (key.equals(InMobiNetworkKeys.AREA_CODE)) {
        if (!"".equals(value)) {
          InMobiSdk.setAreaCode(value);
        }
      } else if (key.equals(InMobiNetworkKeys.AGE)) {
        try {
          if (!"".equals(value)) {
            InMobiSdk.setAge(Integer.parseInt(value));
          }
        } catch (NumberFormatException nfe) {
          Log.d(InMobiMediationAdapter.TAG, "Please Set age properly", nfe);
        }
      } else if (key.equals(InMobiNetworkKeys.POSTAL_CODE)) {
        if (!"".equals(value)) {
          InMobiSdk.setPostalCode(value);
        }
      } else if (key.equals(InMobiNetworkKeys.LANGUAGE)) {
        if (!"".equals(value)) {
          InMobiSdk.setLanguage(value);
        }
      } else if (key.equals(InMobiNetworkKeys.CITY)) {
        city = value;
      } else if (key.equals(InMobiNetworkKeys.STATE)) {
        state = value;
      } else if (key.equals(InMobiNetworkKeys.COUNTRY)) {
        country = value;
      } else if (key.equals(InMobiNetworkKeys.AGE_GROUP)) {
        if (value != null) {
          AgeGroup agegroup = getAgeGroup(value);
          if (agegroup != null) {
            InMobiSdk.setAgeGroup(agegroup);
          }
        }
      } else if (key.equals(InMobiNetworkKeys.EDUCATION)) {
        if (value != null) {
          Education education = getEducation(value);
          if (education != null) {
            InMobiSdk.setEducation(education);
          }
        }
      } else if (key.equals(InMobiNetworkKeys.LOGLEVEL)) {
        if (value != null) {
          InMobiSdk.setLogLevel(getLogLevel(value));
        } else {
          InMobiSdk.setLogLevel(LogLevel.NONE);
        }
      } else if (key.equals(InMobiNetworkKeys.INTERESTS)) {
        InMobiSdk.setInterests(value);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (!Objects.equals(city, "")
          && !Objects.equals(state, "")
          && !Objects.equals(country, "")) {
        InMobiSdk.setLocationWithCityStateCountry(city, state, country);
      }
    }
  }

  static void updateAgeRestrictedUser(MediationAdConfiguration config) {
    if (config.taggedForChildDirectedTreatment()
            == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      InMobiSdk.setIsAgeRestricted(true);
    } else {
      InMobiSdk.setIsAgeRestricted(false);
    }
  }

  static HashMap<String, String> createInMobiParameterMap(MediationAdConfiguration config) {
    HashMap<String, String> map = new HashMap<>();
    map.put("tp", "c_admob");

    if (config.taggedForChildDirectedTreatment()
        == RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      map.put("coppa", "1");
    } else {
      map.put("coppa", "0");
    }

    return map;
  }

  private static AgeGroup getAgeGroup(String value) {
    switch (value) {
      case InMobiNetworkValues.ABOVE_65:
        return AgeGroup.ABOVE_65;
      case InMobiNetworkValues.BELOW_18:
        return AgeGroup.BELOW_18;
      case InMobiNetworkValues.BETWEEN_18_AND_24:
        return AgeGroup.BETWEEN_18_AND_24;
      case InMobiNetworkValues.BETWEEN_25_AND_29:
        return AgeGroup.BETWEEN_25_AND_29;
      case InMobiNetworkValues.BETWEEN_30_AND_34:
        return AgeGroup.BETWEEN_30_AND_34;
      case InMobiNetworkValues.BETWEEN_35_AND_44:
        return AgeGroup.BETWEEN_35_AND_44;
      case InMobiNetworkValues.BETWEEN_45_AND_54:
        return AgeGroup.BETWEEN_45_AND_54;
      case InMobiNetworkValues.BETWEEN_55_AND_65:
        return AgeGroup.BETWEEN_55_AND_65;
    }
    return null;
  }

  private static Education getEducation(String value) {
    switch (value) {
      case InMobiNetworkValues.EDUCATION_COLLEGEORGRADUATE:
        return Education.COLLEGE_OR_GRADUATE;
      case InMobiNetworkValues.EDUCATION_HIGHSCHOOLORLESS:
        return Education.HIGH_SCHOOL_OR_LESS;
      case InMobiNetworkValues.EDUCATION_POSTGRADUATEORABOVE:
        return Education.POST_GRADUATE_OR_ABOVE;
    }
    return null;
  }

  private static LogLevel getLogLevel(String value) {
    if (value.equals(InMobiNetworkValues.LOGLEVEL_DEBUG)) {
      return LogLevel.DEBUG;
    }
    if (value.equals(InMobiNetworkValues.LOGLEVEL_ERROR)) {
      return LogLevel.ERROR;
    }
    if (value.equals(InMobiNetworkValues.LOGLEVEL_NONE)) {
      return LogLevel.NONE;
    }
    return LogLevel.NONE;
  }

  /**
   * Checks whether or not the InMobi native ad has all the required assets.
   *
   * @param nativeAd the InMobi native ad object.
   * @return {@code true} if the native ad has all the required assets.
   */
  public static boolean isValidNativeAd(InMobiNative nativeAd) {
    return nativeAd.getAdCtaText() != null
        && nativeAd.getAdDescription() != null
        && nativeAd.getAdIconUrl() != null
        && nativeAd.getAdLandingPageUrl() != null
        && nativeAd.getAdTitle() != null;
  }

  /**
   * Returns an error code from the corresponding {@link InMobiAdRequestStatus}
   *
   * @param status the InMobi ad request status object.
   * @return the error code.
   */
  public static int getMediationErrorCode(@NonNull InMobiAdRequestStatus status) {
    switch (status.getStatusCode()) {
      case NO_ERROR:
        return 0;
      case NETWORK_UNREACHABLE:
        return 1;
      case NO_FILL:
        return 2;
      case REQUEST_INVALID:
        return 3;
      case REQUEST_PENDING:
        return 4;
      case REQUEST_TIMED_OUT:
        return 5;
      case INTERNAL_ERROR:
        return 6;
      case SERVER_ERROR:
        return 7;
      case AD_ACTIVE:
        return 8;
      case EARLY_REFRESH_REQUEST:
        return 9;
      case AD_NO_LONGER_AVAILABLE:
        return 10;
      case MISSING_REQUIRED_DEPENDENCIES:
        return 11;
      case REPETITIVE_LOAD:
        return 12;
      case GDPR_COMPLIANCE_ENFORCED:
        return 13;
      case GET_SIGNALS_CALLED_WHILE_LOADING:
        return 14;
      case LOAD_WITH_RESPONSE_CALLED_WHILE_LOADING:
        return 15;
      case INVALID_RESPONSE_IN_LOAD:
        return 16;
      case MONETIZATION_DISABLED:
        return 17;
      case CALLED_FROM_WRONG_THREAD:
        return 18;
      case CONFIGURATION_ERROR:
        return 19;
      case LOW_MEMORY:
        return 20;
    }
    // Error '99' to indicate that the error is new and has not been supported by the adapter yet.
    return 99;
  }

}
