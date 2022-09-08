package com.google.ads.mediation.pangle;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;

public class PangleAdapterUtils {

  /**
   * Set the COPPA setting in Pangle SDK.
   *
   * @param coppa an {@code Integer} value that indicates whether the app should be treated as
   *              child-directed for purposes of the COPPA. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE}
   *              means true. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE}
   *              means false. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED}
   *              means unspecified.
   */
  public static int setCoppa(@TagForChildDirectedTreatment int coppa) {
    switch (coppa) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD);
        }
        return PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD);
        }
        return PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD;
      default:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT);
        }
        return PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT;
    }
  }

  /**
   * Set the user data (e.g. in-app purchase status) to be sent to Pangle SDK.
   *
   * @param networkExtras a {@link Bundle} containing optional parameter to be passed to the
   *                      adapter.
   */
  public static void setUserData(@Nullable Bundle networkExtras) {
    if (networkExtras == null || !networkExtras.containsKey(PangleExtras.Keys.USER_DATA)) {
      return;
    }
    PAGConfig.setUserData(networkExtras.getString(PangleExtras.Keys.USER_DATA, ""));
  }
}
