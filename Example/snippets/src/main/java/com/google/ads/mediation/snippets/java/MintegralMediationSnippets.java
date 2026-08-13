/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.snippets.java;

import android.content.Context;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.MBridgeSDKFactory;

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/mintegral and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/mintegral
 */
public class MintegralMediationSnippets {

  private void setDoNotTrackStatus() {
    // [START set_do_not_track_status]
    MBridgeSDK mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();
    mBridgeSDK.setDoNotTrackStatus(false);
    // [END set_do_not_track_status]
  }
}
