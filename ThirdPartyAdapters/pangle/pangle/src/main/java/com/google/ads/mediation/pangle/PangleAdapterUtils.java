package com.google.ads.mediation.pangle;

import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGDoNotSellType;
import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGGDPRConsentType;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;

public class PangleAdapterUtils {

  private static int coppa = -1;
  private static int gdpr = -1;
  private static int ccpa = -1;

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

  /**
   * Set the GDPR setting in Pangle SDK.
   *
   * @param gdpr an {@code Integer} value that indicates whether the user consents the use of
   *             personal data to serve ads under GDPR. See <a href="https://www.pangleglobal.com/integration/android-initialize-pangle-sdk">Pangle's
   *             documentation</a> for more information about what values may be provided.
   */
  public static void setGDPRConsent(@PAGGDPRConsentType int gdpr) {
    if (gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_CONSENT
        && gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_NO_CONSENT
        && gdpr != PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_DEFAULT) {
      // no-op
      Log.w(TAG, "Invalid GDPR value. Pangle SDK only accepts -1, 0 or 1.");
      return;
    }
    if (PAGSdk.isInitSuccess()) {
      PAGConfig.setGDPRConsent(gdpr);
    }
    PangleAdapterUtils.gdpr = gdpr;
  }

  public static int getGdpr() {
    return gdpr;
  }

  /**
   * Set the CCPA setting in Pangle SDK.
   *
   * @param ccpa an {@code Integer} value that indicates whether the user opts in of the "sale" of
   *             the "personal information" under CCPA. See <a href="https://www.pangleglobal.com/integration/android-initialize-pangle-sdk">Pangle's
   *             documentation</a> for more information about what values may be provided.
   */
  public static void setDoNotSell(@PAGDoNotSellType int ccpa) {
    if (ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_SELL
        && ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_NOT_SELL
        && ccpa != PAGDoNotSellType.PAG_DO_NOT_SELL_TYPE_DEFAULT) {
      // no-op
      Log.w(TAG, "Invalid CCPA value. Pangle SDK only accepts -1, 0 or 1.");
      return;
    }
    if (PAGSdk.isInitSuccess()) {
      PAGConfig.setDoNotSell(ccpa);
    }
    PangleAdapterUtils.ccpa = ccpa;
  }

  public static int getCcpa() {
    return ccpa;
  }

  /**
   * Custom parameters for sending Pangle the user details, like in-app purchases status. To enable
   * this function please contact your Pangle's AM for more information.
   *
   * @param networkExtras Custom parameters for sending Pangle the user details.
   */
  public static void setUserData(@Nullable Bundle networkExtras) {
    if (networkExtras == null || !networkExtras.containsKey(PangleExtras.Keys.USER_DATA)) {
      return;
    }
    PAGConfig.setUserData(networkExtras.getString(PangleExtras.Keys.USER_DATA, ""));
  }
}
