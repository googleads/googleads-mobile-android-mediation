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

package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FACEBOOK_INITIALIZATION;

import android.content.Context;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.AudienceNetworkAds.InitResult;
import com.google.android.gms.ads.AdError;
import java.util.ArrayList;

class FacebookInitializer implements AudienceNetworkAds.InitListener {

  private static FacebookInitializer instance;
  private boolean isInitializing = false;
  private boolean isInitialized = false;

  private final ArrayList<Listener> listeners;

  static FacebookInitializer getInstance() {
    if (instance == null) {
      instance = new FacebookInitializer();
    }
    return instance;
  }

  private FacebookInitializer() {
    listeners = new ArrayList<>();
  }

  void initialize(Context context, String placementId, Listener listener) {
    ArrayList<String> placements = new ArrayList<>();
    placements.add(placementId);

    getInstance().initialize(context, placements, listener);
  }

  void initialize(Context context, ArrayList<String> placements, Listener listener) {
    if (isInitializing) {
      listeners.add(listener);
      return;
    }

    if (isInitialized) {
      listener.onInitializeSuccess();
      return;
    }

    isInitializing = true;

    getInstance().listeners.add(listener);
    AudienceNetworkAds.buildInitSettings(context)
        .withMediationService("GOOGLE:" + BuildConfig.ADAPTER_VERSION)
        .withPlacementIds(placements)
        .withInitListener(FacebookInitializer.this)
        .initialize();
  }

  @Override
  public void onInitialized(InitResult initResult) {
    isInitializing = false;
    isInitialized = initResult.isSuccess();

    for (Listener listener : listeners) {
      if (initResult.isSuccess()) {
        listener.onInitializeSuccess();
      } else {
        AdError error = new AdError(ERROR_FACEBOOK_INITIALIZATION, initResult.getMessage(),
            ERROR_DOMAIN);
        listener.onInitializeError(error);
      }
    }
    listeners.clear();
  }

  interface Listener {

    void onInitializeSuccess();

    void onInitializeError(AdError error);
  }

}
