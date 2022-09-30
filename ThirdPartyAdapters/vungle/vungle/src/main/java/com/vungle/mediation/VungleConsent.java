package com.vungle.mediation;

import com.vungle.ads.VungleAds;
import com.vungle.ads.internal.privacy.PrivacyConsent;
import com.vungle.ads.internal.privacy.PrivacyManager;

/**
 * A public static class used to set Vungle Consent Status.
 */
public class VungleConsent {

  /**
   * Update GDPR consent status and corresponding version number.
   */
  public static void updateConsentStatus(
      PrivacyConsent consentStatus, String consentMessageVersion) {
    VungleAds.updateGDPRConsent(consentStatus, consentMessageVersion);
  }

  public static String getCurrentVungleConsent() {
    return PrivacyManager.getConsentStatus();
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return PrivacyManager.getConsentMessageVersion();
  }
}
