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
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

public class UnityInterstitialEventAdapter implements IUnityEventAdapter {

  final MediationInterstitialListener listener;
  final MediationInterstitialAdapter adapter;

  public UnityInterstitialEventAdapter(MediationInterstitialListener listener,
      MediationInterstitialAdapter adapter) {
    this.listener = listener;
    this.adapter = adapter;
  }

  @Override
  public void sendAdEvent(AdEvent adEvent) {
    if (listener == null) {
      return;
    }

    switch (adEvent) {
      case LOADED:
        listener.onAdLoaded(adapter);
        break;
      case OPENED:
        listener.onAdOpened(adapter);
        break;
      case CLICKED:
        listener.onAdClicked(adapter);
        break;
      case CLOSED:
        listener.onAdClosed(adapter);
        break;
      case LEFT_APPLICATION:
        listener.onAdLeftApplication(adapter);
        break;
      default:
        break;
    }
  }
}
