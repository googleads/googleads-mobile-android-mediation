package com.google.ads.mediation.facebook;

import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.facebook.FacebookMediationAdapter.ERROR_FACEBOOK_INITIALIZATION;

import android.content.Context;
import androidx.annotation.NonNull;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.AudienceNetworkAds.InitResult;
import com.google.android.gms.ads.AdError;
import java.util.ArrayList;

class FacebookInitializer implements AudienceNetworkAds.InitListener {

  private static FacebookInitializer instance;
  private boolean mIsInitializing = false;
  private boolean mIsInitialized = false;

  private final ArrayList<Listener> mListeners;

  static FacebookInitializer getInstance() {
    if (instance == null) {
      instance = new FacebookInitializer();
    }
    return instance;
  }

  private FacebookInitializer() {
    mListeners = new ArrayList<>();
  }

  void initialize(Context context, String placementId, Listener listener) {
    ArrayList<String> placements = new ArrayList<>();
    placements.add(placementId);

    getInstance().initialize(context, placements, listener);
  }

  void initialize(@NonNull Context context, @NonNull ArrayList<String> placements,
      @NonNull Listener listener) {
    if (mIsInitializing) {
      mListeners.add(listener);
      return;
    }

    if (mIsInitialized) {
      listener.onInitializeSuccess();
      return;
    }

    mIsInitializing = true;
    getInstance().mListeners.add(listener);
    AudienceNetworkAds.buildInitSettings(context)
        .withMediationService("GOOGLE:" + BuildConfig.ADAPTER_VERSION)
        .withPlacementIds(placements)
        .withInitListener(FacebookInitializer.this)
        .initialize();
  }

  @Override
  public void onInitialized(InitResult initResult) {
    mIsInitializing = false;
    mIsInitialized = initResult.isSuccess();

    for (Listener listener : mListeners) {
      if (initResult.isSuccess()) {
        listener.onInitializeSuccess();
      } else {
        AdError error = new AdError(ERROR_FACEBOOK_INITIALIZATION, initResult.getMessage(),
            ERROR_DOMAIN);
        listener.onInitializeError(error);
      }
    }
    mListeners.clear();
  }

  interface Listener {

    void onInitializeSuccess();

    void onInitializeError(AdError error);
  }

}
