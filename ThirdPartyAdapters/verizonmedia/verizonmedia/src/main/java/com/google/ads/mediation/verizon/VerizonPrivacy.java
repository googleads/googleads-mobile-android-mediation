package com.google.ads.mediation.verizon;

import androidx.annotation.NonNull;
import com.verizon.ads.VASAds;
import java.util.Map;

public class VerizonPrivacy {

  private Map<String, Object> privacyData = null;

  private static final VerizonPrivacy instance = new VerizonPrivacy();

  public static VerizonPrivacy getInstance() {
    return instance;
  }

  private VerizonPrivacy() {
  }

  public void setPrivacyData(@NonNull Map<String, Object> privacyData) {
    this.privacyData = privacyData;
    if (VASAds.isInitialized()) {
      VASAds.setPrivacyData(privacyData);
    }
  }

  public Map<String, Object> getPrivacyData() {
    return privacyData;
  }
}
