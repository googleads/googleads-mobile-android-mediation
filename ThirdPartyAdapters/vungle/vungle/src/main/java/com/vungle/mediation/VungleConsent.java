package com.vungle.mediation;

import com.vungle.warren.Vungle;

/**
 * A public static class used to set Vungle Consent Status.
 */
public class VungleConsent {

  /**
   * Update GDPR consent status and corresponding version number.
   */
  public static void updateConsentStatus(
      Vungle.Consent consentStatus, String consentMessageVersion) {
    Vungle.updateConsentStatus(consentStatus, consentMessageVersion);
  }

}
