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
import android.util.Log;

import com.unity3d.ads.BuildConfig;
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
     * This method will return a
     * {@link com.google.ads.mediation.unity.UnityInitializer} instance.
     *
     * @return the {@link #unityInitializerInstance}.
     *
     */
    static synchronized UnityInitializer getInstance() {
        if (unityInitializerInstance == null) {
            unityInitializerInstance = new UnityInitializer();
        }
        return unityInitializerInstance;
    }

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param context    The context.
     * @param gameId      Unity Ads Game ID.
     * @param initializationListener   Unity Ads Initialization listener.
     *
     */
    public void initializeUnityAds(Context context, String gameId, IUnityAdsInitializationListener
            initializationListener) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "Unity Ads cannot be initialized: current device is not supported.");
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            Log.d(UnityAdapter.TAG, "Unity Ads is already initialized.");
        }

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(context);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", UnityAds.getVersion());
        mediationMetaData.commit();

        UnityAds.initialize(context, gameId, false, true, initializationListener);
    }

}
