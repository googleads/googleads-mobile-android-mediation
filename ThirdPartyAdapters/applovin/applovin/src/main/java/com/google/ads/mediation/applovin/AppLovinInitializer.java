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

import static android.util.Log.DEBUG;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.applovin.mediation.AppLovinUtils.ServerParameterKeys;
import com.applovin.mediation.BuildConfig;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdk.SdkInitializationListener;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;

public class AppLovinInitializer {

  private static AppLovinInitializer instance;
  private final AppLovinSdkWrapper appLovinSdkWrapper;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {
      UNINITIALIZED,
      INITIALIZING,
      INITIALIZED
  })

  public @interface InitializationStatus {

  }

  /**
   * UNINITIALIZED.
   */
  public static final int UNINITIALIZED = 0;
  /**
   * INITIALIZING.
   */
  public static final int INITIALIZING = 1;
  /**
   * INITIALIZED.
   */
  public static final int INITIALIZED = 2;

  private final HashMap<String, Integer> initializationStatus;
  private final HashMap<String, ArrayList<OnInitializeSuccessListener>> initializerListeners;

  private AppLovinInitializer() {
    initializationStatus = new HashMap<>();
    initializerListeners = new HashMap<>();
    appLovinSdkWrapper = new AppLovinSdkWrapper();
  }

  @VisibleForTesting
  AppLovinInitializer(AppLovinSdkWrapper appLovinSdkWrapper) {
    initializationStatus = new HashMap<>();
    initializerListeners = new HashMap<>();
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
    // Initial values
    if (!initializationStatus.containsKey(sdkKey)) {
      initializationStatus.put(sdkKey, UNINITIALIZED);
      initializerListeners.put(sdkKey, new ArrayList<OnInitializeSuccessListener>());
    }

    if (Integer.valueOf(INITIALIZED).equals(initializationStatus.get(sdkKey))) {
      onInitializeSuccessListener.onInitializeSuccess(sdkKey);
      return;
    }

    initializerListeners.get(sdkKey).add(onInitializeSuccessListener);
    if (Integer.valueOf(INITIALIZING).equals(initializationStatus.get(sdkKey))) {
      return;
    }

    initializationStatus.put(sdkKey, INITIALIZING);
    String logMessage = String.format("Attempting to initialize SDK with SDK Key: %s", sdkKey);
    log(DEBUG, logMessage);

    AppLovinSdkSettings sdkSettings = appLovinSdkWrapper.getSdkSettings(context);
    AppLovinSdk sdk = appLovinSdkWrapper.getInstance(sdkKey, sdkSettings, context);
    sdk.setPluginVersion(BuildConfig.ADAPTER_VERSION);
    sdk.setMediationProvider(AppLovinMediationProvider.ADMOB);
    sdk.initializeSdk(new SdkInitializationListener() {
      @Override
      public void onSdkInitialized(AppLovinSdkConfiguration config) {
        // AppLovin currently has no method to check if initialization returned a failure, so assume
        // it is always a success.
        initializationStatus.put(sdkKey, INITIALIZED);

        ArrayList<OnInitializeSuccessListener> listeners = initializerListeners.get(sdkKey);
        if (listeners != null) {
          for (OnInitializeSuccessListener onInitializeSuccessListener : listeners) {
            onInitializeSuccessListener.onInitializeSuccess(sdkKey);
          }
          listeners.clear();
        }
      }
    });
  }

  /**
   * Retrieves the appropriate instance of AppLovin's SDK from the SDK key given in the server
   * parameters, or Android Manifest.
   */
  public AppLovinSdk retrieveSdk(Bundle serverParameters, Context context) {
    String sdkKey =
        (serverParameters != null) ? serverParameters.getString(ServerParameterKeys.SDK_KEY) : null;
    AppLovinSdk sdk;

    AppLovinSdkSettings sdkSettings = appLovinSdkWrapper.getSdkSettings(context);
    if (!TextUtils.isEmpty(sdkKey)) {
      sdk = appLovinSdkWrapper.getInstance(sdkKey, sdkSettings, context);
    } else {
      sdk = appLovinSdkWrapper.getInstance(sdkSettings, context);
    }

    sdk.setPluginVersion(BuildConfig.ADAPTER_VERSION);
    sdk.setMediationProvider(AppLovinMediationProvider.ADMOB);
    return sdk;
  }

  public interface OnInitializeSuccessListener {

    /**
     * Invoked once AppLovin SDK finishes initializing with the specified SDK key.
     */
    void onInitializeSuccess(@NonNull String sdkKey);
  }

}
