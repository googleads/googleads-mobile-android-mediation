package com.vungle.mediation;

import com.vungle.warren.Vungle;

/**
 * Created by akifumi.shinagawa on 5/21/18.
 */

public class VungleConsent {
    private static Vungle.Consent sCurrentVungleConsent;

    public static void updateConsentStatus(Vungle.Consent consentStatus) {
        sCurrentVungleConsent = consentStatus;
        if (Vungle.isInitialized()) {
            Vungle.updateConsentStatus(consentStatus);
        }
    }

    public static Vungle.Consent getCurrentVungleConsent() {
        return sCurrentVungleConsent;
    }
}
