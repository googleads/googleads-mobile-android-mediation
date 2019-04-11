package com.google.ads.mediation.inmobi;

import com.inmobi.sdk.InMobiSdk;

import org.json.JSONObject;

public class InMobiConsent {
    private static JSONObject consentObj = new JSONObject();

    /**
     * Call InMobiConsent.updateGDPRConsent() to update GDPR consent for the user
     * on each request basis.
     */
    public static void updateGDPRConsent(JSONObject consentObj) {
        if (InMobiAdapter.isAppInitialized()) {
            InMobiSdk.updateGDPRConsent(consentObj);
        }
        InMobiConsent.consentObj = consentObj;
    }

    static JSONObject getConsentObj() {
        return consentObj;
    }
}
