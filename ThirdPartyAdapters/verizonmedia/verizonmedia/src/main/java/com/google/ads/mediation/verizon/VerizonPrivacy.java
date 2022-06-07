package com.google.ads.mediation.verizon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.verizon.ads.DataPrivacy;
import com.verizon.ads.VASAds;

public class VerizonPrivacy {

  private DataPrivacy dataPrivacy = null;
  private static final VerizonPrivacy instance = new VerizonPrivacy();

  @NonNull
  public static VerizonPrivacy getInstance() {
    return instance;
  }

  private VerizonPrivacy() {
  }

  public void setDataPrivacy(@NonNull final DataPrivacy dataPrivacy) {
    this.dataPrivacy = dataPrivacy;
    if (VASAds.isInitialized()) {
      VASAds.setDataPrivacy(this.dataPrivacy);
    }
  }

  @Nullable
  public DataPrivacy getDataPrivacy() {
    return dataPrivacy;
  }
}
