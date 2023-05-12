package com.google.ads.mediation.inmobi;

import com.inmobi.sdk.InMobiSdk;

import java.util.Map;

public class InMobiSdkWrapper {

  public String getToken(final Map<String, String> extras, final String keywords) {
    return InMobiSdk.getToken(extras, keywords);
  }
}
