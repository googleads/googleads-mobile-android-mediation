package com.google.ads.mediation.inmobi;

import androidx.annotation.NonNull;
import com.inmobi.sdk.InMobiSdk;

import java.util.Map;

/**
 A wrapper class to enable mocking the static methods on InMobi SDK for adapter unit tests.
 */
public class InMobiSdkWrapper {

  public String getToken(final Map<String, String> extras, final String keywords) {
    return InMobiSdk.getToken(extras, keywords);
  }

  @NonNull
  public String getVersion() {
    return InMobiSdk.getVersion();
  }

  public boolean isSDKInitialized() {
    return InMobiSdk.isSDKInitialized();
  }
}
