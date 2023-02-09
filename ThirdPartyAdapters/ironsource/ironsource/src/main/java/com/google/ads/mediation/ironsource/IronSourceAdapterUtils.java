package com.google.ads.mediation.ironsource;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.ironsource.mediationsdk.ISBannerSize;

import java.util.ArrayList;

/**
 * The {@link IronSourceAdapterUtils} class provides the publisher an ability to pass Activity to
 * IronSource SDK, as well as some helper methods for the IronSource adapters.
 */
public class IronSourceAdapterUtils {

  /**
   * Adapter class name for logging.
   */
  static final String TAG = IronSourceMediationAdapter.class.getSimpleName();

  /**
   * Key to obtain App Key, required for initializing IronSource SDK.
   */
  static final String KEY_APP_KEY = "appKey";

  /**
   * Key to obtain the IronSource Instance ID, required to show IronSource ads.
   */
  static final String KEY_INSTANCE_ID = "instanceId";

  /**
   * Default IronSource instance ID.
   */
  static final String DEFAULT_INSTANCE_ID = "0";

  /**
   * Constant used for IronSource internal reporting.
   */
  static final String MEDIATION_NAME = "AdMob";

  /**
   * Constant used for IronSource adapter version internal reporting
   */
  static final String ADAPTER_VERSION_NAME = "400";

  /**
   * UI thread handler used to send callbacks with AdMob interface.
   */
  private static Handler uiHandler;

  static synchronized void sendEventOnUIThread(Runnable runnable) {
    if (uiHandler == null) {
      uiHandler = new Handler(Looper.getMainLooper());
    }

    uiHandler.post(runnable);
  }

  @Nullable
  public static ISBannerSize getISBannerSize(@NonNull Context context,
                                             @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    potentials.add(AdSize.LARGE_BANNER);

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize != null) {
      if (AdSize.BANNER.equals(closestSize)){
        return ISBannerSize.BANNER;
      } else if(AdSize.MEDIUM_RECTANGLE.equals(closestSize)){
        return ISBannerSize.RECTANGLE;
      } else if(AdSize.LARGE_BANNER.equals(closestSize)){
        return ISBannerSize.LARGE;
      }

      return new ISBannerSize(closestSize.getWidth(), closestSize.getHeight());
    }

    return null;
  }

}
