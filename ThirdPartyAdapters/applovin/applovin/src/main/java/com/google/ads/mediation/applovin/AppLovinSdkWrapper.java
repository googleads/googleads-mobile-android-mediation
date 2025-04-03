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
import com.applovin.sdk.AppLovinSdk;

/**
 * Wrapper class to enable mocking of AppLovin SDK for unit testing.
 *
 * <p><b>Note:</b> It is used as a layer between the AppLovin Adapter's and the AppLovin SDK. It is
 * required to use this class instead of calling the AppLovin SDK methods directly.
 */
public class AppLovinSdkWrapper {

  public AppLovinSdk getInstance(Context context) {
    return AppLovinSdk.getInstance(context);
  }

  public String getSdkVersion() {
    return AppLovinSdk.VERSION;
  }
}
