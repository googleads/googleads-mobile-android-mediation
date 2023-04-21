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

package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.vungle.mediation.VungleNetworkSettings;
import com.vungle.warren.InitCallback;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitCallback {

  private static final VungleInitializer instance = new VungleInitializer();
  private final AtomicBoolean isInitializing = new AtomicBoolean(false);
  private final ArrayList<VungleInitializationListener> initListeners;
  private final Handler handler = new Handler(Looper.getMainLooper());

  @NonNull
  public static VungleInitializer getInstance() {
    return instance;
  }

  private VungleInitializer() {
    initListeners = new ArrayList<>();
    Plugin.addWrapperInfo(
        VungleApiClient.WrapperFramework.admob,
        com.vungle.mediation.BuildConfig.ADAPTER_VERSION.replace('.', '_'));
  }

  public void initialize(
      final @NonNull String appId,
      final @NonNull Context context,
      @NonNull VungleInitializationListener listener) {

    if (Vungle.isInitialized()) {
      listener.onInitializeSuccess();
      return;
    }

    if (isInitializing.getAndSet(true)) {
      initListeners.add(listener);
      return;
    }

    // Keep monitoring VungleSettings in case of any changes we need to re-init SDK to apply
    // updated settings.
    VungleNetworkSettings.setVungleSettingsChangedListener(
        new VungleNetworkSettings.VungleSettingsChangedListener() {
          @Override
          public void onVungleSettingsChanged(@NonNull VungleSettings settings) {
            // Ignore if sdk is yet to initialize, it will get considered while init.
            if (!Vungle.isInitialized()) {
              return;
            }

            // Pass new settings to SDK.
            updateCoppaStatus(
                MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
            Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this, settings);
          }
        });

    updateCoppaStatus(MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());

    VungleSettings vungleSettings = VungleNetworkSettings.getVungleSettings();
    Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this, vungleSettings);
    initListeners.add(listener);
  }

  @Override
  public void onSuccess() {
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            for (VungleInitializationListener listener : initListeners) {
              listener.onInitializeSuccess();
            }
            initListeners.clear();
          }
        });
    isInitializing.set(false);
  }

  @Override
  public void onError(final VungleException exception) {
    final AdError error = VungleMediationAdapter.getAdError(exception);
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            for (VungleInitializationListener listener : initListeners) {
              listener.onInitializeError(error);
            }
            initListeners.clear();
          }
        });
    isInitializing.set(false);
  }

  @Override
  public void onAutoCacheAdAvailable(String placementId) {
    // Unused
  }

  public void updateCoppaStatus(int configuration) {
    switch (configuration) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        Vungle.updateUserCoppaStatus(true);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        Vungle.updateUserCoppaStatus(false);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        // Vungle SDK only supports updating a user's COPPA status with true and false
        // values. If you haven't specified how you would like your content treated with
        // respect to COPPA in ad requests, you must indicate in the Liftoff Monetize Publisher
        // Dashboard whether your app is directed toward children under age 13.
        break;
    }
  }

  public interface VungleInitializationListener {

    void onInitializeSuccess();

    void onInitializeError(AdError error);
  }
}
