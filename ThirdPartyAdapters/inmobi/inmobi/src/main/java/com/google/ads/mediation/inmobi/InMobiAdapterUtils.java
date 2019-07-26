package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.AgeGroup;
import com.inmobi.sdk.InMobiSdk.Education;
import com.inmobi.sdk.InMobiSdk.Gender;
import com.inmobi.sdk.InMobiSdk.LogLevel;
import java.util.ArrayList;
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

    static void setGlobalTargeting(MediationAdRequest mediationAdRequest, Bundle extras) {
        configureGlobalTargeting(extras);

        if (mediationAdRequest.getLocation() != null) {
            InMobiSdk.setLocation(mediationAdRequest.getLocation());
        }

        // Date Of Birth
        if (mediationAdRequest.getBirthday() != null) {
            Calendar dob = Calendar.getInstance();
            dob.setTime(mediationAdRequest.getBirthday());
            InMobiSdk.setYearOfBirth(dob.get(Calendar.YEAR));
        }

        // Gender
        if (mediationAdRequest.getGender() != -1) {
            switch (mediationAdRequest.getGender()) {
                case AdRequest.GENDER_MALE:
                    InMobiSdk.setGender(Gender.MALE);
                    break;
                case AdRequest.GENDER_FEMALE:
                    InMobiSdk.setGender(Gender.FEMALE);
                    break;
                default:
                    break;
            }
        }
    }

    static void setGlobalTargeting(MediationRewardedAdConfiguration configuration, Bundle extras) {
        configureGlobalTargeting(extras);

        if (configuration.getLocation() != null) {
            InMobiSdk.setLocation(configuration.getLocation());
        }
    }

    private static void configureGlobalTargeting(Bundle extras) {
        if (extras == null) {
            Log.d("InMobiAdapter", "Bundle extras are null");
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
                    Log.d("Please Set age properly", nfe.getMessage());
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

    static HashMap<String, String> createInMobiParameterMap(MediationAdRequest adRequest) {
        HashMap<String, String> map = new HashMap<>();
        map.put("tp", "c_admob");

        if (adRequest.taggedForChildDirectedTreatment()
                == MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            map.put("coppa", "1");
        } else {
            map.put("coppa", "0");
        }

        return map;
    }

    static HashMap<String, String> createInMobiParameterMap(MediationAdConfiguration config) {
        HashMap<String, String> map = new HashMap<>();
        map.put("tp", "c_admob");

        if (config.taggedForChildDirectedTreatment()
                == MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
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

    static <T> T mandatoryChecking(@Nullable T x, String param) throws MandatoryParamException {
        if (x == null || x.toString().isEmpty()) {
            throw new MandatoryParamException("Mandatory param " + param + " not present");
        }
        return x;
    }

    // Start of helper code to remove when available in SDK

    /**
     * Find the closest supported AdSize from the list of potentials to the provided size. Returns
     * null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
        original = new AdSize(actualWidth, actualHeight);

        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    private static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK
}
