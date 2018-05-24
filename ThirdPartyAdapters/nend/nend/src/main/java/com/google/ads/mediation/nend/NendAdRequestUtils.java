package com.google.ads.mediation.nend;

import android.support.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;

import net.nend.android.NendAdUserFeature;

import java.util.Calendar;
import java.util.Date;

/*
 * A helper class used by the {@link NendAdapter} to get information about an ad request.
 */
class NendAdRequestUtils {

    // Because the start of Calendar and Date 's month is 0, nendSDK has a -1 offset.
    private static final int CALENDAR_MONTH_OFFSET = 1;

    @Nullable
    static NendAdUserFeature createUserFeature(MediationAdRequest mediationAdRequest) {

        if (mediationAdRequest == null) {
            return null;
        }

        Date birthday = mediationAdRequest.getBirthday();

        NendAdUserFeature.Gender gender = null;
        switch (mediationAdRequest.getGender()) {
            case AdRequest.GENDER_MALE:
                gender = NendAdUserFeature.Gender.MALE;
                break;
            case AdRequest.GENDER_FEMALE:
                gender = NendAdUserFeature.Gender.FEMALE;
                break;
        }

        if (birthday == null && gender == null) {
            return null;
        }

        NendAdUserFeature.Builder builder = new NendAdUserFeature.Builder();
        if (birthday != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(birthday);
            builder.setBirthday(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)
                    + CALENDAR_MONTH_OFFSET, cal.get(Calendar.DAY_OF_MONTH));
        }
        if (gender != null) {
            builder.setGender(gender);
        }

        return builder.build();
    }
}