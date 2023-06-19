// Copyright 2023 Google LLC
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

package com.google.ads.mediation.applovin;

import android.content.Context;
import androidx.annotation.NonNull;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;

/**
 * Wrapper class to enable mocking of {@link AppLovinAds#initializeSdk} for unit testing.
 *
 * <p><b>Note:</b> It is used as a layer between the AppLovin Adapter's and the AppLovin SDK. It is
 * required to use this class instead of calling the AppLovin SDK methods directly. More background:
 * http://yaqs/6706506443522048
 */
public class AppLovinSdkWrapper {

  private AppLovinSdkSettings appLovinSdkSettings;

  public AppLovinSdk getInstance(AppLovinSdkSettings sdkSettings, Context context) {
    return AppLovinSdk.getInstance(sdkSettings, context);
  }

  public AppLovinSdk getInstance(String sdkKey, AppLovinSdkSettings sdkSettings, Context context) {
    return AppLovinSdk.getInstance(sdkKey, sdkSettings, context);
  }

  @NonNull
  public AppLovinSdkSettings getSdkSettings(@NonNull Context context) {
    if (appLovinSdkSettings == null) {
      appLovinSdkSettings = new AppLovinSdkSettings(context);
    }
    return appLovinSdkSettings;
  }
}
