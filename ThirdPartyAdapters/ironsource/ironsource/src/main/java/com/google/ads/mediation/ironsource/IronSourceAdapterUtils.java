package com.google.ads.mediation.ironsource;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.AdapterError;
import com.ironsource.mediationsdk.logger.IronSourceError;

/**
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK, as well as some helper methods for the IronSource adapters.
 */
public class IronSourceAdapterUtils {

  /** Adapter class name for logging. */
  static final String TAG = IronSourceMediationAdapter.class.getSimpleName();

  /** Key to obtain App Key, required for initializing IronSource SDK. */
  static final String KEY_APP_KEY = "appKey";

  /** Key to obtain the IronSource Instance ID, required to show IronSource ads. */
  static final String KEY_INSTANCE_ID = "instanceId";

  /** Default IronSource instance ID. */
  static final String DEFAULT_INSTANCE_ID = "0";

  /** Constant used for IronSource internal reporting. */
  static final String MEDIATION_NAME = "AdMob";

  /** Constant used for IronSource adapter version internal reporting */
  static final String ADAPTER_VERSION_NAME = "310";

  /** UI thread handler used to send callbacks with AdMob interface. */
  private static Handler uiHandler;

  static synchronized void sendEventOnUIThread(Runnable runnable) {
    if (uiHandler == null) {
      uiHandler = new Handler(Looper.getMainLooper());
    }
    uiHandler.post(runnable);
  }

  /**
   * Creates a formatted adapter error string from the specified error code and description.
   *
   * @param code the error code.
   * @param description the error message.
   * @return the formatted error string.
   */
  @NonNull
  public static String createAdapterError(@AdapterError int code, @NonNull String description) {
    return String.format("%d: %s", code, description);
  }

  /**
   * Creates a formatted SDK error string from the specified {@link IronSourceError}.
   *
   * @param ironSourceError the IronSource error object.
   * @return the formatted error string.
   */
  public static String createSDKError(IronSourceError ironSourceError) {
    return String.format(
        "%d: %s", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage());
  }
}
