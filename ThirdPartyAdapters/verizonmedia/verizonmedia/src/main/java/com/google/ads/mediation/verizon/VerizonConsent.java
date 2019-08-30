package com.google.ads.mediation.verizon;

import androidx.annotation.NonNull;

import com.verizon.ads.VASAds;

import java.util.Map;

public class VerizonConsent {

    private Map<String, String> consentMap = null;
    private boolean restricted = false;

    private static final VerizonConsent instance = new VerizonConsent();

    public static VerizonConsent getInstance() {
        return instance;
    }

    private VerizonConsent() {
    }

    public void setConsentData(@NonNull Map<String, String> consentMap, boolean restricted) {
        this.consentMap = consentMap;
        this.restricted = restricted;
        if (VASAds.isInitialized()) {
            VASAds.setConsentData(consentMap, restricted);
        }
    }

    public Map<String, String> getConsentMap() {
        return consentMap;
    }

    public boolean isRestricted() {
        return restricted;
    }
}
