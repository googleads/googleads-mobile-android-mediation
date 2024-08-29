package com.google.ads.mediation.ironsource;

public class IronSourceConstants {

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
   * Default non bidder IronSource instance ID.
   */
  static final String DEFAULT_NON_RTB_INSTANCE_ID = "0";

  /**
   * Constant used for IronSource internal reporting.
   */
  static final String MEDIATION_NAME = "AdMob";

  /**
   * Constant used for IronSource adapter version internal reporting.
   */
  static final String ADAPTER_VERSION_NAME = "500";

  static final String WATERMARK = "google_watermark";

}
