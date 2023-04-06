package com.vungle.mediation;

import com.vungle.ads.VunglePrivacySettings;

/**
 * A public static class used to set Vungle Consent Status.
 */
public class VungleConsent {

  public static void setGDPRStatus(
      Boolean optedIn, String consentMessageVersion) {
    VunglePrivacySettings.setGDPRStatus(optedIn, consentMessageVersion);
  }

  public static String getGDPRStatus() {
    return VunglePrivacySettings.getGDPRStatus();
  }

  public static String getGDPRMessageVersion() {
    return VunglePrivacySettings.getGDPRMessageVersion();
  }

  public static String getCcpaStatus() {
    return VunglePrivacySettings.getCCPAStatus();
  }

  public static void setCCPAStatus(Boolean optedIn) {
    VunglePrivacySettings.setCCPAStatus(optedIn);
  }

  public static void publishAndroidId(Boolean publishAndroidId) {
    VunglePrivacySettings.setPublishAndroidId(publishAndroidId);
  }

}
