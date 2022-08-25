// Copyright 2022 Google Inc.
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

package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostAdapterUtils.createSDKError;
import static com.google.ads.mediation.chartboost.ChartboostAdapter.TAG;

import android.content.Context;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.COPPA;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * The {@link ChartboostInitializer} class is used to handle initialization process.
 */
public class ChartboostInitializer {

  private static ChartboostInitializer instance;

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

  private @InitializationStatus
  int initializationStatus;

  private final ArrayList<Listener> mListeners = new ArrayList<>();

  private ChartboostInitializer() {
    initializationStatus = UNINITIALIZED;
  }

  public static ChartboostInitializer getInstance() {
    if (instance == null) {
      instance = new ChartboostInitializer();
    }
    return instance;
  }

  public void init(@NonNull final Context context,
      @NonNull ChartboostParams params, @NonNull final Listener listener) {

    if (initializationStatus == INITIALIZED) {
      listener.onInitializationSucceeded();
      return;
    }

    mListeners.add(listener);
    if (initializationStatus == INITIALIZING) {
      return;
    }

    initializationStatus = INITIALIZING;

    updateCoppaStatus(context,
        MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
    Chartboost.startWithAppId(context, params.getAppId(), params.getAppSignature(),
        new StartCallback() {
          @Override
          public void onStartCompleted(@Nullable StartError startError) {
            ArrayList<Listener> tempListeners = new ArrayList<>(mListeners);
            if (startError == null) {
              Log.d(TAG, "Chartboost SDK initialized.");

              initializationStatus = INITIALIZED;
              for (Listener initListener : tempListeners) {
                initListener.onInitializationSucceeded();
              }
            } else {
              initializationStatus = UNINITIALIZED;

              AdError initializationError = createSDKError(startError);
              for (Listener initListener : tempListeners) {
                initListener.onInitializationFailed(initializationError);
              }
            }
            mListeners.clear();
          }
        });
  }

  public void updateCoppaStatus(Context context, int configuration) {
    switch (configuration) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        Chartboost.addDataUseConsent(context, new COPPA(true));
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        Chartboost.addDataUseConsent(context, new COPPA(false));
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        // Chartboost's SDK only supports updating a user's COPPA status with true and false values
        break;
    }
  }

  interface Listener {

    /**
     * Called when the Chartboost SDK initializes successfully.
     */
    void onInitializationSucceeded();

    /**
     * Called when the Chartboost SDK fails to initialize.
     *
     * @param error the initialization error.
     */
    void onInitializationFailed(@NonNull AdError error);
  }
}
