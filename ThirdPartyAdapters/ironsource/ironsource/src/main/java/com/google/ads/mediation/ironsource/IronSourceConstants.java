package com.google.ads.mediation.ironsource;

import android.app.Activity;

public class IronSourceConstants {

  /** Adapter class name for logging. */
  static final String TAG = IronSourceMediationAdapter.class.getSimpleName();

  /** Key to obtain App Key, required for initializing ironSource SDK. */
  static final String KEY_APP_KEY = "appKey";

  /** Key to obtain the IronSource Instance ID, required to show IronSource ads. */
  static final String KEY_INSTANCE_ID = "instanceId";

  /** Default IronSource instance ID. */
  static final String DEFAULT_INSTANCE_ID = "0";

  /** Constant used for IronSource internal reporting. */
  static final String MEDIATION_NAME = "AdMob";

  /** Constant used for IronSource adapter version internal reporting. */
  static final String ADAPTER_VERSION_NAME = "400";

  /** IronSource adapter error domain. */
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.ironsource";

  /** IronSource SDK error domain. */
  public static final String IRONSOURCE_SDK_ERROR_DOMAIN = "com.ironsource.mediationsdk";

  // region Error codes
  /** Server parameters (e.g. instance ID) are nil. */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /** IronSource requires an {@link Activity} context to initialize their SDK. */
  public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 102;

  /** IronSource can only load 1 ad per IronSource instance ID. */
  public static final int ERROR_AD_ALREADY_LOADED = 103;

  /** Banner size mismatch. */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 105;

  // endregion
}
