package com.google.ads.mediation.pangle;

import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;

public class PangleAdapterUtils {

  private static int coppa = -1;

  /**
   * Set the COPPA setting in Pangle SDK.
   *
   * @param coppa an {@code Integer} value that indicates whether the app should be treated as
   *              child-directed for purposes of the COPPA. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE}
   *              means true. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE}
   *              means false. {@link RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED}
   *              means unspecified.
   */
  public static void setCoppa(@TagForChildDirectedTreatment int coppa) {
    switch (coppa) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD);
        }
        PangleAdapterUtils.coppa = 1;
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD);
        }
        PangleAdapterUtils.coppa = 0;
        break;
      default:
        if (PAGSdk.isInitSuccess()) {
          PAGConfig.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT);
        }
        PangleAdapterUtils.coppa = -1;
        break;
    }
  }

  public static int getCoppa() {
    return coppa;
  }
}
