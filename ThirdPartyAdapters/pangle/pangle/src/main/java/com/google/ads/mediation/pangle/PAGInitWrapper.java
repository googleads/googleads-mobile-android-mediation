package com.google.ads.mediation.pangle;

import android.content.Context;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback;

/**
 * A wrapper for Pangle's {@link PAGSdk#init(Context, PAGConfig, PAGInitCallback)}.
 */
public class PAGInitWrapper {

  public void init(Context context, PAGConfig config, PAGSdk.PAGInitCallback callback) {
    PAGSdk.init(context, config, callback);
  }
}
