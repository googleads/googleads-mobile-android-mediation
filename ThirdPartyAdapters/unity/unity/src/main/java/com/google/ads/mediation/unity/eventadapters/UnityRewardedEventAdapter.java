// Copyright 2021 Google LLC
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

package com.google.ads.mediation.unity.eventadapters;

import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent;
import com.google.ads.mediation.unity.UnityReward;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;

public class UnityRewardedEventAdapter implements IUnityEventAdapter {

  final MediationRewardedAdCallback listener;

  public UnityRewardedEventAdapter(MediationRewardedAdCallback listener) {
    this.listener = listener;
  }

  @Override
  public void sendAdEvent(AdEvent adEvent) {
    if (listener == null) {
      return;
    }

    switch (adEvent) {
      case OPENED:
        listener.onAdOpened();
        break;
      case CLICKED:
        listener.reportAdClicked();
        break;
      case CLOSED:
        listener.onAdClosed();
        break;
      case IMPRESSION:
        listener.reportAdImpression();
        break;
      case VIDEO_START:
        listener.onVideoStart();
        break;
      case REWARD:
        // Unity Ads doesn't provide a reward value. The publisher is expected to
        // override the reward in AdMob console.
        listener.onUserEarnedReward(new UnityReward());
        break;
      case VIDEO_COMPLETE:
        listener.onVideoComplete();
        break;
      default:
        break;
    }
  }
}
