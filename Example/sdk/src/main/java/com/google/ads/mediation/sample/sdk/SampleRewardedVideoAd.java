/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.sdk;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The {@link SampleRewardedVideoAd} class is a simple rewarded video ad object. The sample
 * rewarded video ad is parcelable, so an instance of it can be passed between activities.
 */
public class SampleRewardedVideoAd implements Parcelable {

    /**
     * A {@link Creator}, needed for an object to be parcelable.
     */
    public static final Creator<SampleRewardedVideoAd> CREATOR =
            new Creator<SampleRewardedVideoAd>() {
                @Override
                public SampleRewardedVideoAd createFromParcel(Parcel in) {
                    return new SampleRewardedVideoAd(in);
                }

                @Override
                public SampleRewardedVideoAd[] newArray(int size) {
                    return new SampleRewardedVideoAd[size];
                }
            };

    /**
     * A simple name for the rewarded video ad.
     */
    private String mAdName;

    /**
     * The rewarded amount associated with this native ad.
     */
    private int mRewardAmount;

    /**
     * @return {@link #mAdName}.
     */
    public String getAdName() {
        return this.mAdName;
    }

    /**
     * @return {@link #mRewardAmount}.
     */
    public int getRewardAmount() {
        return this.mRewardAmount;
    }

    /**
     * Constructor with parcel needed for an object to be parcelable.
     *
     * @param in an Android {@link Parcel} object.
     */
    public SampleRewardedVideoAd(Parcel in) {
        this.mAdName = in.readString();
        this.mRewardAmount = in.readInt();
    }

    /**
     * The default constructor for sample rewarded video ad. Requires a name and reward amount.
     *
     * @param adName       a simple name for the rewarded video ad.
     * @param rewardAmount reward amount to be provided when a user completes watching this ad.
     */
    SampleRewardedVideoAd(String adName, int rewardAmount) {
        this.mAdName = adName;
        this.mRewardAmount = rewardAmount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAdName);
        dest.writeInt(mRewardAmount);
    }
}
