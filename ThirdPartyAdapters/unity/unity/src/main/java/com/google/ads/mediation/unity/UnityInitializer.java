// Copyright 2016 Google LLC
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

package com.google.ads.mediation.unity;

import androidx.annotation.VisibleForTesting;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.UnityAds;

/**
 * The {@link UnityInitializer} is used to initialize Unity ads
 */
public class UnityInitializer {

  /**
   * UnityInitializer instance.
   */
  private static UnityInitializer unityInitializerInstance;

  private final UnityAdsWrapper unityAdsWrapper;

  /**
   * Returns a {@link com.google.ads.mediation.unity.UnityInitializer} instance.
   *
   * @return the {@link #unityInitializerInstance}.
   */
  static synchronized UnityInitializer getInstance() {
    if (unityInitializerInstance == null) {
      unityInitializerInstance = new UnityInitializer();
    }
    return unityInitializerInstance;
  }

  private UnityInitializer() {
    unityAdsWrapper = new UnityAdsWrapper();
  }

  @VisibleForTesting
  UnityInitializer(UnityAdsWrapper unityAdsWrapper) {
    this.unityAdsWrapper = unityAdsWrapper;
  }

  /**
   * Initializes {@link UnityAds}. In the case of multiple initialize calls UnityAds will call the
   * appropriate functions provided in the IUnityAdsInitializationListener after initialization is
   * complete.
   *
   * @param gameId                 Unity Ads Game ID.
   * @param initializationListener Unity Ads Initialization listener.
   */
  public void initializeUnityAds(String gameId, InitializationListener
      initializationListener) {

    if (unityAdsWrapper.isInitialized()) {
      // Unity Ads is already initialized.
      initializationListener.onInitializationComplete(null);
      return;
    }

    // UnityAdsWrapper now handles mediation info via InitializationConfiguration
    unityAdsWrapper.initialize(gameId, initializationListener);
  }
}
