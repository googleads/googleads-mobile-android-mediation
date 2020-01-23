// Copyright 2016 Google Inc.
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

import android.app.Activity;
import android.util.Log;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

/**
 * The {@link UnitySingleton} class is used to load {@link UnityAds}, handle multiple
 * {@link UnityAdapter} instances and mediate their callbacks.
 */
public final class UnitySingleton {
    /**
     * The only instance of
     * {@link com.google.ads.mediation.unity.UnitySingleton}.
     */
    private static UnitySingleton unitySingletonInstance;

    /**
     * This method will return a
     * {@link com.google.ads.mediation.unity.UnitySingleton} instance.
     *
     * @return the {@link #unitySingletonInstance}.
     */
    public static UnitySingleton getInstance() {
        if (unitySingletonInstance == null) {
            unitySingletonInstance = new UnitySingleton();
        }
        return unitySingletonInstance;
    }

    private UnitySingleton() {
    }

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param activity    The Activity context.
     * @param gameId      Unity Ads Game ID.
     * @return {@code true} if the {@link UnityAds} has initialized successfully, {@code false}
     * otherwise.
     */
    public boolean initializeUnityAds(Activity activity, String gameId) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            return false;
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            return true;
        }

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", "3.3.0");
        mediationMetaData.commit();

        UnityAds.initialize(activity, gameId, false, true);

        return true;
    }
}
