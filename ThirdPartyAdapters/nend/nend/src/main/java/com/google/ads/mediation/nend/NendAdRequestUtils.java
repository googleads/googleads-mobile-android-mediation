package com.google.ads.mediation.nend;

import android.support.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;

import net.nend.android.NendAdUserFeature;

import java.util.Calendar;
import java.util.Date;

class NendAdRequestUtils {

    private static final int CALENDAR_MONTH_ADJUST_VALUE = 1;  // Because it adjusts -1 on nendSDK.

    @Nullable
    static NendAdUserFeature createUserFeature(MediationAdRequest mediationAdRequest) {

        if (mediationAdRequest == null) {
            return null;
        }

        Date birthday = mediationAdRequest.getBirthday();

        NendAdUserFeature.Gender gender = null;
        if (mediationAdRequest.getGender() != AdRequest.GENDER_UNKNOWN) {
            switch (mediationAdRequest.getGender()) {
                case AdRequest.GENDER_MALE:
                    gender = NendAdUserFeature.Gender.MALE;
                    break;
                case AdRequest.GENDER_FEMALE:
                    gender = NendAdUserFeature.Gender.FEMALE;
                    break;
            }
        }

        if (birthday == null && gender == null) {
            return null;
        }

        NendAdUserFeature.Builder builder = new NendAdUserFeature.Builder();
        if (birthday != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(birthday);
            builder.setBirthday(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + CALENDAR_MONTH_ADJUST_VALUE, cal.get(Calendar.DAY_OF_MONTH));
        }
        if (gender != null) {
            builder.setGender(gender);
        }

        return builder.build();
    }
}