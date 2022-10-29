package com.vungle.mediation;

import androidx.annotation.Nullable;
import com.vungle.warren.Vungle;

/**
 * A public static class used to set Vungle Consent Status.
 */
public class VungleConsent {

  /**
   * Update GDPR consent status and corresponding version number.
   */
  public static void updateConsentStatus(
      Consent consentStatus, String consentMessageVersion) {
    Vungle.Consent consent = mapConsentToVungle(consentStatus);
    if (consent != null) {
      Vungle.updateConsentStatus(consent, consentMessageVersion);
    }
  }

  public static Consent getCurrentVungleConsent() {
    return mapConsentToAdMob(Vungle.getConsentStatus());
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return Vungle.getConsentMessageVersion();
  }

  public static void updateCCPAStatus(Consent ccpaStatus) {
    Vungle.Consent consent = mapConsentToVungle(ccpaStatus);
    if (consent != null) {
      Vungle.updateCCPAStatus(consent);
    }
  }

  public static Consent getCCPAStatus() {
    return mapConsentToAdMob(Vungle.getCCPAStatus());
  }

  private static Vungle.Consent mapConsentToVungle(Consent consent) {
    switch (consent) {
      case OPTED_IN:
        return Vungle.Consent.OPTED_IN;
      case OPTED_OUT:
        return Vungle.Consent.OPTED_OUT;
    }
    return null;
  }

  private static Consent mapConsentToAdMob(@Nullable Vungle.Consent consent) {
    if (consent == null) {
      return null;
    }
    switch (consent) {
      case OPTED_IN:
        return Consent.OPTED_IN;
      case OPTED_OUT:
        return Consent.OPTED_OUT;
    }
    return null;
  }

  public enum Consent {
    OPTED_IN,
    OPTED_OUT
  }

}
