package com.google.ads.mediation.yahoo;

import com.yahoo.ads.DataPrivacy;
import com.yahoo.ads.YASAds;


public class YahooPrivacy {

  private DataPrivacy dataPrivacy = null;

  private static final YahooPrivacy instance = new YahooPrivacy();

  public static YahooPrivacy getInstance() {
    return instance;
  }

  private YahooPrivacy() {
  }

  public void setDataPrivacy(final DataPrivacy dataPrivacy) {
    this.dataPrivacy = dataPrivacy;
    if (YASAds.isInitialized()) {
      YASAds.setDataPrivacy(this.dataPrivacy);
    }
  }

  public DataPrivacy getDataPrivacy() {
    return dataPrivacy;
  }
}