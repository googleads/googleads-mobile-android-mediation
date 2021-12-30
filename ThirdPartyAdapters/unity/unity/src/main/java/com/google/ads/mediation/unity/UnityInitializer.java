// Copyright 2020 Google Inc.
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

import android.content.Context;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

/**
 * The {@link UnityInitializer} is used to initialize Unity ads
 */
public class UnityInitializer {

  /**
   * UnityInitializer instance.
   */
  private static UnityInitializer unityInitializerInstance;

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

  /**
   * Initializes {@link UnityAds}. In the case of multiple initialize calls UnityAds will call the
   * appropriate functions provided in the IUnityAdsInitializationListener after initialization is
   * complete.
   *
   * @param context                The context.
   * @param gameId                 Unity Ads Game ID.
   * @param initializationListener Unity Ads Initialization listener.
   */
  public void initializeUnityAds(Context context, String gameId, IUnityAdsInitializationListener
      initializationListener) {

    if (UnityAds.isInitialized()) {
      // Unity Ads is already initialized.
      initializationListener.onInitializationComplete();
      return;
    }

    // Set mediation meta data before initializing.
    MediationMetaData mediationMetaData = new MediationMetaData(context);
    mediationMetaData.setName("AdMob");
    mediationMetaData.setVersion(UnityAds.getVersion());
    mediationMetaData.set("adapter_version", BuildConfig.ADAPTER_VERSION);
    mediationMetaData.commit();

    UnityAds.initialize(context, gameId, false, initializationListener);
  }
}
