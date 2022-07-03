package com.google.ads.mediation.applovin;

import static android.util.Log.DEBUG;
import static com.applovin.mediation.ApplovinAdapter.log;

import android.content.Context;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import com.applovin.mediation.BuildConfig;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdk.SdkInitializationListener;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;

public class AppLovinInitializer {

  private static AppLovinInitializer instance;

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

  private final HashMap<String, Integer> initializationStatus;
  private final HashMap<String, ArrayList<OnInitializeSuccessListener>> initializerListeners;

  private AppLovinInitializer() {
    initializationStatus = new HashMap<>();
    initializerListeners = new HashMap<>();
  }

  public static AppLovinInitializer getInstance() {
    if (instance == null) {
      instance = new AppLovinInitializer();
    }
    return instance;
  }

  public void initialize(@NonNull Context context, @NonNull final String sdkKey,
      @NonNull OnInitializeSuccessListener onInitializeSuccessListener) {
    // Initial values
    if (!initializationStatus.containsKey(sdkKey)) {
      initializationStatus.put(sdkKey, UNINITIALIZED);
      initializerListeners.put(sdkKey, new ArrayList<OnInitializeSuccessListener>());
    }

    if (Integer.valueOf(INITIALIZED).equals(initializationStatus.get(sdkKey))) {
      onInitializeSuccessListener.onInitializeSuccess(sdkKey);
      return;
    }

    initializerListeners.get(sdkKey).add(onInitializeSuccessListener);
    if (Integer.valueOf(INITIALIZING).equals(initializationStatus.get(sdkKey))) {
      return;
    }

    initializationStatus.put(sdkKey, INITIALIZING);
    String logMessage = String.format("Attempting to initialize SDK with SDK Key: %s", sdkKey);
    log(DEBUG, logMessage);

    AppLovinSdkSettings sdkSettings = AppLovinMediationAdapter.getSdkSettings(context);
    AppLovinSdk sdk = AppLovinSdk.getInstance(sdkKey, sdkSettings, context);
    sdk.setPluginVersion(BuildConfig.ADAPTER_VERSION);
    sdk.setMediationProvider(AppLovinMediationProvider.ADMOB);
    sdk.initializeSdk(new SdkInitializationListener() {
      @Override
      public void onSdkInitialized(AppLovinSdkConfiguration config) {
        // AppLovin currently has no method to check if initialization returned a failure, so assume
        // it is always a success.
        initializationStatus.put(sdkKey, INITIALIZED);

        ArrayList<OnInitializeSuccessListener> listeners = initializerListeners.get(sdkKey);
        if (listeners != null) {
          for (OnInitializeSuccessListener onInitializeSuccessListener : listeners) {
            onInitializeSuccessListener.onInitializeSuccess(sdkKey);
          }
          listeners.clear();
        }
      }
    });
  }

  public interface OnInitializeSuccessListener {

    /**
     * Invoked once AppLovin SDK finishes initializing with the specified SDK key.
     */
    void onInitializeSuccess(@NonNull String sdkKey);
  }

}
