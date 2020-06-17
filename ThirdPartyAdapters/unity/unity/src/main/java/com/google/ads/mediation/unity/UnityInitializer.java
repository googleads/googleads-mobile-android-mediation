package com.google.ads.mediation.unity;

import android.app.Activity;
import android.util.Log;

import com.unity3d.ads.BuildConfig;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

/**
 * The {@link UnityInitializer} is used to initialize Unity ads
 */
public class UnityInitializer {

    private static UnityInitializer unityInitializerInstance;

    /**
     * This method will return a
     * {@link com.google.ads.mediation.unity.UnityInitializer} instance.
     *
     * @return the {@link #unityInitializerInstance}.
     *
     */
    static UnityInitializer getInstance() {
        if (unityInitializerInstance == null) {
            unityInitializerInstance = new UnityInitializer();
        }
        return unityInitializerInstance;
    }

    /**
     * This method will initialize {@link UnityAds}.
     *
     * @param activity    The Activity context.
     * @param gameId      Unity Ads Game ID.
     * @param initializationListener   Unity Ads Initialization listener.
     *
     */
    public void initializeUnityAds(Activity activity, String gameId, IUnityAdsInitializationListener
            initializationListener) {
        // Check if the current device is supported by Unity Ads before initializing.
        if (!UnityAds.isSupported()) {
            Log.w(UnityAdapter.TAG, "The current device is not supported by Unity Ads.");
            initializationListener.onInitializationFailed(UnityAds.UnityAdsInitializationError.INTERNAL_ERROR,
                    "The current device is not supported by Unity Ads.");
        }

        if (UnityAds.isInitialized()) {
            // Unity Ads is already initialized.
            initializationListener.onInitializationComplete();
        }

        // Set mediation meta data before initializing.
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("AdMob");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.set("adapter_version", "3.3.0");
        mediationMetaData.commit();

        UnityAds.initialize(activity, gameId, false, true, initializationListener);
    }

}
