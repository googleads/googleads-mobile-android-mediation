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

/**
 * The {@link SampleRewardedAdListener} class listens for rewarded video ad events. These ad events
 * more or less represent the events that a typical ad network would provide.
 */
public interface SampleRewardedAdListener {

  /** Called when the rewarded ad is loaded. */
  public void onRewardedAdLoaded();

  /**
   * Called when the rewarded ad fails to load.
   *
   * @param error reason for failure.
   */
  public void onRewardedAdFailedToLoad(SampleErrorCode error);

  /**
   * Called when the user is eligible for a reward.
   *
   * @param rewardType the reward type to be sent.
   * @param amount the reward amount to be rewarded.
   */
  public void onAdRewarded(String rewardType, int amount);

  /** Called when the ad is clicked. */
  public void onAdClicked();

  /** Called when an ad goes full screen. */
  public void onAdFullScreen();

  /** Called when an ad is closed. */
  public void onAdClosed();

  /** Called when an ad is finished. */
  public void onAdCompleted();
}
