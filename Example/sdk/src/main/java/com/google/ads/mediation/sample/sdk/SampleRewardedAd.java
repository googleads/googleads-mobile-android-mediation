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

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.ads.mediation.sample.sdk.activities.SampleSDKAdsActivity;
import java.util.Random;

/**
 * The {@link SampleRewardedAd} class is used to load and show rewarded ad for the Sample SDK.
 */
public class SampleRewardedAd implements Parcelable {

  /**
   * A {@link Creator}, needed for an object to be parcelable.
   */
  public static final Creator<SampleRewardedAd> CREATOR =
      new Creator<SampleRewardedAd>() {
        @Override
        public SampleRewardedAd createFromParcel(Parcel in) {
          return new SampleRewardedAd(in.readString());
        }

        @Override
        public SampleRewardedAd[] newArray(int size) {
          return new SampleRewardedAd[size];
        }
      };

  /**
   * Ad Unit ID to initialize a Sample Rewarded Ad.
   */
  private final String adUnitId;

  /**
   * A flag that indicates whether a rewarded ad is ready to show.
   */
  private boolean isAdAvailable;

  /**
   * A listener to forward any rewarded ad events.
   */
  private SampleRewardedAdListener listener;

  /**
   * The reward amount associated with the ad.
   */
  private int reward;

  /**
   * Construct a rewarded ad.
   */
  public SampleRewardedAd(String adUnitId) {
    this.adUnitId = adUnitId;
  }

  /**
   * Sets the rewarded ad listener to which the rewarded ad events will be forwarded.
   *
   * @param listener a {@code SampleRewardedAdListener} to which to forward the rewarded video ad
   * events.
   */
  public void setListener(SampleRewardedAdListener listener) {
    this.listener = listener;
  }

  /**
   * Returns a rewarded ad listener to forward the rewarded events.
   *
   * @return a {@code SampleRewardedAdListener} that is currently registered to the Sample SDK.
   */
  public SampleRewardedAdListener getListener() {
    return listener;
  }

  /**
   * Returns if the rewarded ad is available to show.
   */
  public boolean isAdAvailable() {
    return isAdAvailable;
  }

  /**
   * Gets the reward for this rewarded ad. Returns 0 until an ad is available.
   */
  public int getReward() {
    return reward;
  }

  /**
   * Loads a rewarded ad.
   */
  public void loadAd(SampleAdRequest request) {
    Random random = new Random();
    int nextInt = random.nextInt(100);
    SampleErrorCode errorCode = null;
    if (nextInt < 80) {
      reward = 5;
      isAdAvailable = true;
      if (listener != null) {
        listener.onRewardedAdLoaded();
      }
    } else if (nextInt < 85) {
      errorCode = SampleErrorCode.UNKNOWN;
    } else if (nextInt < 90) {
      errorCode = SampleErrorCode.BAD_REQUEST;
    } else if (nextInt < 95) {
      errorCode = SampleErrorCode.NETWORK_ERROR;
    } else if (nextInt < 100) {
      errorCode = SampleErrorCode.NO_INVENTORY;
    }
    if (errorCode != null && listener != null && !isAdAvailable) {
      listener.onRewardedAdFailedToLoad(errorCode);
    }
  }

  /**
   * Shows a rewarded ad if one is available. The publisher should check if {@link #isAdAvailable()}
   * is {@code true} before calling this method.
   */
  public void showAd(Activity activity) {
    if (!(isAdAvailable())) {
      Log.w(
          "SampleSDK",
          "No ads to show. Call SampleRewardedAd.isAdAvailable() before " + "calling showAd().");
      return;
    }

    if (activity == null) {
      Log.d("SampleSDK", "Current activity is null. Make sure to pass in a valid activity.");
      return;
    }

    Intent intent = new Intent(activity, SampleSDKAdsActivity.class);
    intent.putExtra(SampleSDKAdsActivity.KEY_REWARDED_AD_EXTRA, this);
    activity.startActivity(intent);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(adUnitId);
  }
}

