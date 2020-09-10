package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.vungle.mediation.VungleConsent;
import com.vungle.mediation.VungleNetworkSettings;
import com.vungle.warren.InitCallback;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitCallback {

  private static final VungleInitializer instance = new VungleInitializer();
  private AtomicBoolean mIsInitializing = new AtomicBoolean(false);

  private ArrayList<VungleInitializationListener> mInitListeners;

  private Handler mHandler = new Handler(Looper.getMainLooper());

  public static VungleInitializer getInstance() {
    return instance;
  }

  private VungleInitializer() {
    mInitListeners = new ArrayList<>();
    Plugin.addWrapperInfo(
        VungleApiClient.WrapperFramework.admob,
        com.vungle.mediation.BuildConfig.VERSION_NAME.replace('.', '_'));
  }

  public void initialize(
      final String appId, final Context context, VungleInitializationListener listener) {

    if (Vungle.isInitialized()) {
      listener.onInitializeSuccess();
      return;
    }

    if (mIsInitializing.getAndSet(true)) {
      mInitListeners.add(listener);
      return;
    }

    // Keep monitoring VungleSettings in case of any changes we need to re-init SDK to apply
    // updated settings.
    VungleNetworkSettings.setVungleSettingsChangedListener(
        new VungleNetworkSettings.VungleSettingsChangedListener() {
          @Override
          public void onVungleSettingsChanged(VungleSettings updatedSettings) {
            // Ignore if sdk is yet to initialize, it will get considered while init
            if (!Vungle.isInitialized()) {
              return;
            }

            VungleSettings settings =
                (updatedSettings != null) ? updatedSettings : new VungleSettings.Builder().build();
            // Pass new settings to SDK.
            Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this, settings);
          }
        });

    VungleSettings vungleSettings = VungleNetworkSettings.getVungleSettings();
    if (vungleSettings == null) {
      vungleSettings = new VungleSettings.Builder().build();
    }
    Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this, vungleSettings);
    mInitListeners.add(listener);
  }

  @Override
  public void onSuccess() {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (VungleConsent.getCurrentVungleConsent() != null) {
              Vungle.updateConsentStatus(
                  VungleConsent.getCurrentVungleConsent(),
                  VungleConsent.getCurrentVungleConsentMessageVersion());
            }
            for (VungleInitializationListener listener : mInitListeners) {
              listener.onInitializeSuccess();
            }
            mInitListeners.clear();
          }
        });
    mIsInitializing.set(false);
  }

  @Override
  public void onError(final VungleException throwable) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            for (VungleInitializationListener listener : mInitListeners) {
              listener.onInitializeError(throwable.getLocalizedMessage());
            }
            mInitListeners.clear();
          }
        });
    mIsInitializing.set(false);
  }

  @Override
  public void onAutoCacheAdAvailable(String placementId) {
    // Unused
  }

  public interface VungleInitializationListener {

    void onInitializeSuccess();

    void onInitializeError(String errorMessage);
  }
}
