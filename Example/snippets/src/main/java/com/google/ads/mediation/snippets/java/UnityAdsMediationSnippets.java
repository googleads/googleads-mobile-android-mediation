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

import android.app.Activity;
import com.unity3d.ads.metadata.MetaData;

/**
 * Java code snippets for https://developers.google.com/admob/android/mediation/unity and
 * https://developers.google.com/ad-manager/mobile-ads-sdk/android/mediation/unity
 */
public class UnityAdsMediationSnippets extends Activity {

  private void setGdprMetaData() {
    // [START set_gdpr_meta_data]
    MetaData gdprMetaData = new MetaData(this);
    gdprMetaData.set("gdpr.consent", true);
    gdprMetaData.commit();
    // [END set_gdpr_meta_data]
  }

  private void setCcpaMetaData() {
    // [START set_ccpa_meta_data]
    MetaData ccpaMetaData = new MetaData(this);
    ccpaMetaData.set("privacy.consent", true);
    ccpaMetaData.commit();
    // [END set_ccpa_meta_data]
  }
}
