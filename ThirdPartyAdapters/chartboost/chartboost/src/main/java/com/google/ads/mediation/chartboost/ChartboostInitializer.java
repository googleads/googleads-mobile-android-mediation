package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.COPPA;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import java.util.ArrayList;

/**
 * The {@link ChartboostInitializer} class is used to handle initialization process.
 */
public class ChartboostInitializer {

  private static ChartboostInitializer instance;

  private boolean isInitializing = false;
  private boolean isInitialized = false;
  private final ArrayList<Listener> initListeners = new ArrayList<>();

  public static ChartboostInitializer getInstance() {
    if (instance == null) {
      instance = new ChartboostInitializer();
    }
    return instance;
  }

  public void initialize(@NonNull final Context context,
      @NonNull ChartboostParams chartboostParams, @NonNull final Listener listener) {
    if (isInitializing) {
      initListeners.add(listener);
      return;
    }

    if (isInitialized) {
      listener.onInitializationSucceeded();
      return;
    }

    isInitializing = true;
    initListeners.add(listener);

    ChartboostAdapterUtils.updateCoppaStatus(context,
        MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
    Chartboost.startWithAppId(context, chartboostParams.getAppId(),
        chartboostParams.getAppSignature(),
        new StartCallback() {
          @Override
          public void onStartCompleted(@Nullable StartError startError) {
            isInitializing = false;
            if (startError == null) {
              isInitialized = true;
              Log.d(TAG, "Chartboost SDK initialized.");
              for (Listener initListener : initListeners) {
                initListener.onInitializationSucceeded();
              }
            } else {
              isInitialized = false;
              AdError initializationError = ChartboostConstants.createSDKError(startError);
              for (Listener initListener : initListeners) {
                initListener.onInitializationFailed(initializationError);
              }
            }
            initListeners.clear();
          }
        });
  }

  interface Listener {

    /**
     * Called when the Chartboost SDK initializes successfully.
     */
    void onInitializationSucceeded();

    /**
     * Called when the Chartboost SDK fails to initialize.
     *
     * @param error the initialization error.
     */
    void onInitializationFailed(@NonNull AdError error);
  }
}
