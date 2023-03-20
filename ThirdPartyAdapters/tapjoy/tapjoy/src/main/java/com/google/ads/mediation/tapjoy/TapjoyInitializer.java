// Copyright 2019 Google LLC
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

package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.util.Log;
import com.tapjoy.TJConnectListener;
import com.tapjoy.Tapjoy;
import java.util.ArrayList;
import java.util.Hashtable;

public class TapjoyInitializer implements TJConnectListener {

  private static TapjoyInitializer instance;
  private InitStatus status;

  private final ArrayList<Listener> initListeners;

  private enum InitStatus {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED
  }

  static TapjoyInitializer getInstance() {
    if (instance == null) {
      instance = new TapjoyInitializer();
    }
    return instance;
  }

  private TapjoyInitializer() {
    initListeners = new ArrayList<>();
    status = InitStatus.UNINITIALIZED;
  }

  void initialize(Activity activity, String sdkKey, Hashtable<String, Object> connectFlags,
      Listener listener) {
    if (status.equals(InitStatus.INITIALIZED) || Tapjoy.isConnected()) {
      listener.onInitializeSucceeded();
      return;
    }

    initListeners.add(listener);
    if (!status.equals(InitStatus.INITIALIZING)) {
      status = InitStatus.INITIALIZING;

      Log.i(TapjoyMediationAdapter.TAG, "Connecting to Tapjoy for Tapjoy-AdMob adapter.");
      Tapjoy.connect(activity, sdkKey, connectFlags, TapjoyInitializer.this);
    }
  }

  @Override
  public void onConnectSuccess() {
    status = InitStatus.INITIALIZED;

    for (Listener listener : initListeners) {
      listener.onInitializeSucceeded();
    }
    initListeners.clear();
  }

  @Override
  public void onConnectFailure() {
    status = InitStatus.UNINITIALIZED;

    for (Listener listener : initListeners) {
      listener.onInitializeFailed("Tapjoy failed to connect.");
    }
    initListeners.clear();
  }

  interface Listener {

    void onInitializeSucceeded();

    void onInitializeFailed(String message);
  }

}
