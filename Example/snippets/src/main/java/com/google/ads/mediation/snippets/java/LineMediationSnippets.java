/*
 * Copyright 2025 Google LLC
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

import android.os.Bundle;
import com.google.ads.mediation.line.LineExtras;
import com.google.ads.mediation.line.LineMediationAdapter;
import com.google.android.gms.ads.AdRequest;

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/line and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/line
 */
public class LineMediationSnippets {

  private void setTestMode() {
    // [START set_test_mode]
    LineMediationAdapter.Companion.setTestMode(true);
    // [END set_test_mode]
  }

  private void setNetworkSpecificParams() {
    // [START set_network_specific_params]
    LineExtras lineExtras = new LineExtras(/* enableAdSound: */ true);
    Bundle extras = lineExtras.build();

    AdRequest request =
        new AdRequest.Builder().addNetworkExtrasBundle(LineMediationAdapter.class, extras).build();
    // [END set_network_specific_params]
  }
}
