package com.vungle.mediation;

import com.vungle.warren.Vungle;

/** A public static class used to set Vungle Consent Status. */
public class VungleConsent {

  private static Vungle.Consent sCurrentVungleConsent = null;
  private static String sCurrentVungleConsentMessageVersion = "";

  /** Update GDPR consent status and corresponding version number */
  public static void updateConsentStatus(
      Vungle.Consent consentStatus, String consentMessageVersion) {
    sCurrentVungleConsent = consentStatus;
    sCurrentVungleConsentMessageVersion = consentMessageVersion;

    if (Vungle.isInitialized()
        && sCurrentVungleConsent != null
        && sCurrentVungleConsentMessageVersion != null) {
      Vungle.updateConsentStatus(sCurrentVungleConsent, sCurrentVungleConsentMessageVersion);
    }
  }

  public static Vungle.Consent getCurrentVungleConsent() {
    return sCurrentVungleConsent;
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return sCurrentVungleConsentMessageVersion;
  }
}
