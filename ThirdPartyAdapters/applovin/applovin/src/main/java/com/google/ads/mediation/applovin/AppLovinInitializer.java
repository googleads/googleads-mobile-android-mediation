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

package com.google.ads.mediation.applovin;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.applovin.mediation.BuildConfig;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdk.SdkInitializationListener;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;

public class AppLovinInitializer {

  private static final String TAG = AppLovinInitializer.class.getSimpleName();

  private static AppLovinInitializer instance;
  private final AppLovinSdkWrapper appLovinSdkWrapper;

  private AppLovinInitializer() {
    appLovinSdkWrapper = new AppLovinSdkWrapper();
  }

  @VisibleForTesting
  AppLovinInitializer(AppLovinSdkWrapper appLovinSdkWrapper) {
    this.appLovinSdkWrapper = appLovinSdkWrapper;
  }

  public static AppLovinInitializer getInstance() {
    if (instance == null) {
      instance = new AppLovinInitializer();
    }
    return instance;
  }

  public void initialize(@NonNull Context context, @NonNull final String sdkKey,
      @NonNull OnInitializeSuccessListener onInitializeSuccessListener) {
    AppLovinSdk sdk = appLovinSdkWrapper.getInstance(context);
    AppLovinSdkInitializationConfiguration initConfig =
        AppLovinSdkInitializationConfiguration.builder(sdkKey)
            .setMediationProvider(AppLovinMediationProvider.ADMOB)
            .setPluginVersion(BuildConfig.ADAPTER_VERSION)
            .build();
    sdk.initialize(
        initConfig,
        new SdkInitializationListener() {
          @Override
          public void onSdkInitialized(AppLovinSdkConfiguration config) {
            onInitializeSuccessListener.onInitializeSuccess();
          }
        });
  }

  // TODO: Refactor the adapter so that callers of this method directly call
  // appLovinSdkWrapper.getInstance(context) instead.
  public AppLovinSdk retrieveSdk(Context context) {
    return appLovinSdkWrapper.getInstance(context);
  }

  public interface OnInitializeSuccessListener {

    /** Invoked once AppLovin SDK finishes initializing. */
    void onInitializeSuccess();
  }

}
