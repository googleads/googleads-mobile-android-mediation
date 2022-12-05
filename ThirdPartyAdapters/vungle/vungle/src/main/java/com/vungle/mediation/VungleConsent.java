package com.vungle.mediation;

import androidx.annotation.NonNull;
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
      VungleConsentStatus consentStatus, String consentMessageVersion) {
    if (consentStatus == null) {
      return;
    }

    PrivacyConsent consent = mappingFromMediation(consentStatus);
    VungleAds.updateGDPRConsent(consent, consentMessageVersion);
  }

  public static String getCurrentVungleConsent() {
    return PrivacyManager.getConsentStatus();
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return PrivacyManager.getConsentMessageVersion();
  }

  public static String getCcpaStatus() {
    return PrivacyManager.getCcpaStatus();
  }

  public static void updateCCPAStatus(VungleConsentStatus consentStatus) {
    if (consentStatus == null) {
      return;
    }

    PrivacyConsent consent = mappingFromMediation(consentStatus);
    VungleAds.updateCCPAStatus(consent);
  }

  public static String getCoppaStatus() {
    return PrivacyManager.getCoppaStatus().name();
  }

  public static void updateUserCoppaStatus(boolean userCoppaStatus) {
    VungleAds.updateUserCoppaStatus(userCoppaStatus);
  }

  public enum VungleConsentStatus {
    OPTED_IN, OPTED_OUT
  }

  private static PrivacyConsent mappingFromMediation(@NonNull VungleConsentStatus consentStatus) {
    return consentStatus == VungleConsentStatus.OPTED_IN ? PrivacyConsent.OPT_IN
        : PrivacyConsent.OPT_OUT;
  }

}
