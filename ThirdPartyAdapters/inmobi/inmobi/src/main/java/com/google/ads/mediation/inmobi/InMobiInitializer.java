// Copyright 2020 Google LLC
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

package com.google.ads.mediation.inmobi;

import static com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION;
import static com.google.ads.mediation.inmobi.InMobiMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Size;
import com.google.android.gms.ads.AdError;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class InMobiInitializer {

  private static InMobiInitializer instance;

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

  private final ArrayList<Listener> listeners = new ArrayList<>();

  private InMobiInitializer() {
    initializationStatus = UNINITIALIZED;
  }

  public static InMobiInitializer getInstance() {
    if (instance == null) {
      instance = new InMobiInitializer();
    }
    return instance;
  }

  public void init(@NonNull final Context context,
      @NonNull @Size(min = 32, max = 36) String accountID, @NonNull final Listener listener) {

    if (initializationStatus == INITIALIZED) {
      listener.onInitializeSuccess();
      return;
    }

    listeners.add(listener);
    if (initializationStatus == INITIALIZING) {
      return;
    }

    initializationStatus = INITIALIZING;

    InMobiSdk.init(context, accountID, InMobiConsent.getConsentObj(),
        new SdkInitializationListener() {
          @Override
          public void onInitializationComplete(Error error) {
            if (error == null) {
              Log.d(TAG, "InMobi SDK initialized.");

              initializationStatus = INITIALIZED;
              for (Listener initListener : listeners) {
                initListener.onInitializeSuccess();
              }
            } else {
              initializationStatus = UNINITIALIZED;

              AdError initializationError = InMobiConstants.createAdapterError(
                  ERROR_INMOBI_FAILED_INITIALIZATION, error.getLocalizedMessage());
              for (Listener initListener : listeners) {
                initListener.onInitializeError(initializationError);
              }
            }
            listeners.clear();
          }
        });
  }

  interface Listener {

    /**
     * Called when the InMobi SDK initializes successfully.
     */
    void onInitializeSuccess();

    /**
     * Called when the InMobi SDK fails to initialize.
     *
     * @param error the initialization error.
     */
    void onInitializeError(@NonNull AdError error);
  }
}
