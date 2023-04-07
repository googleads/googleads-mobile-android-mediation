// Copyright 2018 Google LLC
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

package com.google.ads.mediation.ironsource;

import android.os.Handler;
import android.os.Looper;

/**
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK, as well as some helper methods for the IronSource adapters.
 */
public class IronSourceAdapterUtils {

  /**
   * Adapter class name for logging.
   */
  static final String TAG = IronSourceMediationAdapter.class.getSimpleName();

  /**
   * Key to obtain App Key, required for initializing IronSource SDK.
   */
  static final String KEY_APP_KEY = "appKey";

  /**
   * Key to obtain the IronSource Instance ID, required to show IronSource ads.
   */
  static final String KEY_INSTANCE_ID = "instanceId";

  /**
   * Default IronSource instance ID.
   */
  static final String DEFAULT_INSTANCE_ID = "0";

  /**
   * Constant used for IronSource internal reporting.
   */
  static final String MEDIATION_NAME = "AdMob";

  /**
   * Constant used for IronSource adapter version internal reporting
   */
  static final String ADAPTER_VERSION_NAME = "310";

  /**
   * UI thread handler used to send callbacks with AdMob interface.
   */
  private static Handler uiHandler;

  static synchronized void sendEventOnUIThread(Runnable runnable) {
    if (uiHandler == null) {
      uiHandler = new Handler(Looper.getMainLooper());
    }
    uiHandler.post(runnable);
  }
}
