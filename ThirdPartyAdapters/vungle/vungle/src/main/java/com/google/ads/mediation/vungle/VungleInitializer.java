package com.google.ads.mediation.vungle;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleAds.WrapperFramework;
import com.vungle.ads.VungleException;
import com.vungle.mediation.VungleConsent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitializationListener {

  private static final VungleInitializer instance = new VungleInitializer();
  private final AtomicBoolean isInitializing = new AtomicBoolean(false);
  private final ArrayList<VungleInitializationListener> initListeners;

  @NonNull
  public static VungleInitializer getInstance() {
    return instance;
  }

  private VungleInitializer() {
    initListeners = new ArrayList<>();
    VungleAds.setIntegrationName(
        WrapperFramework.admob,
        com.vungle.mediation.BuildConfig.ADAPTER_VERSION.replace('.', '_'));
  }

  public void initialize(
      final @NonNull String appId,
      final @NonNull Context context,
      @NonNull VungleInitializationListener listener) {

    if (VungleAds.isInitialized()) {
      listener.onInitializeSuccess();
      return;
    }

    if (isInitializing.getAndSet(true)) {
      initListeners.add(listener);
      return;
    }

    updateCoppaStatus(MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());

    VungleAds.init(context, appId, VungleInitializer.this);
    initListeners.add(listener);
  }

  @Override
  public void onSuccess() {
    for (VungleInitializationListener listener : initListeners) {
      listener.onInitializeSuccess();
    }
    initListeners.clear();
    isInitializing.set(false);
  }

  @Override
  public void onError(@NonNull final VungleException exception) {
    final AdError error = VungleMediationAdapter.getAdError(exception);
    for (VungleInitializationListener listener : initListeners) {
      listener.onInitializeError(error);
    }
    initListeners.clear();
    isInitializing.set(false);
  }

  public void updateCoppaStatus(int configuration) {
    switch (configuration) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        VungleConsent.setCOPPAStatus(true);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        VungleConsent.setCOPPAStatus(false);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        // Vungle's SDK only supports updating a user's COPPA status with true and false
        // values. If you haven't specified how you would like your content treated with
        // respect to COPPA in ad requests, you must indicate in the Vungle Publisher
        // Dashboard whether your app is directed toward children under age 13.
        break;
    }
  }

  public interface VungleInitializationListener {

    void onInitializeSuccess();

    void onInitializeError(AdError error);
  }
}
