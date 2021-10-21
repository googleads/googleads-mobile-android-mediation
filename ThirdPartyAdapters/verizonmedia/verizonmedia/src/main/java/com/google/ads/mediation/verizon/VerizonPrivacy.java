package com.google.ads.mediation.verizon;

import com.verizon.ads.DataPrivacy;
import com.verizon.ads.VASAds;


public class VerizonPrivacy {

  private DataPrivacy dataPrivacy = null;

  private static final VerizonPrivacy instance = new VerizonPrivacy();

  public static VerizonPrivacy getInstance() {
    return instance;
  }

  private VerizonPrivacy() {
  }

  public void setDataPrivacy(final DataPrivacy dataPrivacy) {
    this.dataPrivacy = dataPrivacy;
    if (VASAds.isInitialized()) {
      VASAds.setDataPrivacy(this.dataPrivacy);
    }
  }

  public DataPrivacy getDataPrivacy() {
    return dataPrivacy;
  }
}
