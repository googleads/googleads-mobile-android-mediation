package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.COPPA;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

  public void init(@NonNull final Context context,
      @NonNull ChartboostParams params, @NonNull final Listener listener) {
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

    updateCoppaStatus(context,
        MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
    Chartboost.startWithAppId(context, params.getAppId(), params.getAppSignature(),
        new StartCallback() {
          @Override
          public void onStartCompleted(@Nullable StartError startError) {
            isInitializing = false;
            ArrayList<Listener> tempListeners = new ArrayList<>(initListeners);
            if (startError == null) {
              isInitialized = true;
              Log.d(TAG, "Chartboost SDK initialized.");
              for (Listener initListener : tempListeners) {
                initListener.onInitializationSucceeded();
              }
            } else {
              isInitialized = false;
              AdError initializationError = ChartboostConstants.createSDKError(startError);
              for (Listener initListener : tempListeners) {
                initListener.onInitializationFailed(initializationError);
              }
            }
            initListeners.clear();
          }
        });
  }

  public void updateCoppaStatus(Context context, int configuration) {
    switch (configuration) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        Chartboost.addDataUseConsent(context, new COPPA(true));
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        Chartboost.addDataUseConsent(context, new COPPA(false));
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        // Chartboost's SDK only supports updating a user's COPPA status with true and false values.
        break;
    }
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
