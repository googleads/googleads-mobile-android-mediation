package com.google.ads.mediation.pangle;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback;
import com.google.android.gms.ads.AdError;
import java.util.ArrayList;

public class PangleInitializer implements PAGInitCallback {

  private static PangleInitializer instance;

  private boolean isInitializing = false;
  private boolean isInitialized = false;
  private final ArrayList<Listener> initListeners;

  @NonNull
  public static PangleInitializer getInstance() {
    if (instance == null) {
      instance = new PangleInitializer();
    }
    return instance;
  }

  private PangleInitializer() {
    initListeners = new ArrayList<>();
  }

  public void initialize(@NonNull Context context, @NonNull String appId,
      @NonNull Listener listener) {

    if (TextUtils.isEmpty(appId)) {
      AdError error = PangleConstants.createAdapterError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to initialize Pangle SDK. Missing or invalid App ID.");
      Log.w(PangleMediationAdapter.TAG, error.toString());
      listener.onInitializeError(error);
      return;
    }

    if (isInitializing) {
      initListeners.add(listener);
      return;
    }

    if (isInitialized) {
      listener.onInitializeSuccess();
      return;
    }

    isInitializing = true;
    initListeners.add(listener);

    // Pangle SDK is only initialized using a single App ID.
    PAGConfig adConfig = new PAGConfig.Builder()
        .appId(appId)
        .setChildDirected(PangleAdapterUtils.getCoppa())
        .setGDPRConsent(PangleMediationAdapter.getGDPRConsent())
        .setDoNotSell(PangleMediationAdapter.getDoNotSell())
        .build();
    PAGSdk.init(context, adConfig, PangleInitializer.this);
  }

  @Override
  public void success() {
    isInitializing = false;
    isInitialized = true;

    for (Listener listener : initListeners) {
      listener.onInitializeSuccess();
    }
    initListeners.clear();
  }

  @Override
  public void fail(int errorCode, @NonNull String errorMessage) {
    isInitializing = false;
    isInitialized = false;

    AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
    for (Listener listener : initListeners) {
      listener.onInitializeError(error);
    }
    initListeners.clear();
  }

  public interface Listener {
    void onInitializeSuccess();

    void onInitializeError(@NonNull AdError error);
  }
}
