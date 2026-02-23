// Copyright 2022 Google LLC
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

package com.google.ads.mediation.pangle;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback;
import com.google.android.gms.ads.AdError;
import java.util.ArrayList;

/**
 * Manages initializing Pangle SDK.
 *
 * <p>The adapter should only use this class for initializing Pangle SDK instead of directly
 * calling
 * Pangle SDK's init() method.
 */
public class PangleInitializer implements PAGInitCallback {

  private static PangleInitializer instance;

  private boolean isInitializing = false;
  private boolean isInitialized = false;
  private final ArrayList<Listener> initListeners;

  private final PangleSdkWrapper pangleSdkWrapper;
  private final PangleFactory pangleFactory;

  @NonNull
  public static PangleInitializer getInstance() {
    if (instance == null) {
      instance = new PangleInitializer();
    }
    return instance;
  }

  private PangleInitializer() {
    initListeners = new ArrayList<>();
    pangleSdkWrapper = new PangleSdkWrapper();
    pangleFactory = new PangleFactory();
  }

  @VisibleForTesting
  public PangleInitializer(PangleSdkWrapper pangleSdkWrapper, PangleFactory pangleFactory) {
    initListeners = new ArrayList<>();
    this.pangleSdkWrapper = pangleSdkWrapper;
    this.pangleFactory = pangleFactory;
  }

  public void initialize(
      @NonNull Context context, @NonNull String appId, @NonNull Listener listener) {

    if (TextUtils.isEmpty(appId)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to initialize Pangle SDK. Missing or invalid App ID.");
      Log.w(PangleMediationAdapter.TAG, error.toString());
      listener.onInitializeError(error);
      return;
    }

    if (isInitializing) {
      initListeners.add(listener);
      return;
    }

    if (isInitialized) {
      listener.onInitializeSuccess();
      return;
    }

    isInitializing = true;
    initListeners.add(listener);

    // Pangle SDK is only initialized using a single App ID.
    PAGConfig adConfig =
        pangleFactory
            .createPAGConfigBuilder()
            .appId(appId)
            .setAdxId(PangleConstants.ADX_ID)
            .setUserData(
                String.format(
                    "[{\"name\":\"mediation\",\"value\":\"google\"},{\"name\":\"adapter_version\",\"value\":\"%s\"}]",
                    BuildConfig.ADAPTER_VERSION))
            .build();
    pangleSdkWrapper.init(context, adConfig, PangleInitializer.this);
  }

  @Override
  public void success() {
    isInitializing = false;
    isInitialized = true;

    for (Listener listener : initListeners) {
      listener.onInitializeSuccess();
    }
    initListeners.clear();
  }

  @Override
  public void fail(int errorCode, @NonNull String errorMessage) {
    isInitializing = false;
    isInitialized = false;

    AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
    for (Listener listener : initListeners) {
      listener.onInitializeError(error);
    }
    initListeners.clear();
  }

  public interface Listener {

    void onInitializeSuccess();

    void onInitializeError(@NonNull AdError error);
  }
}
