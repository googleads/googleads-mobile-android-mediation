package com.google.ads.mediation.inmobi;

import android.content.Context;
import androidx.annotation.NonNull;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;
import java.util.Map;
import org.json.JSONObject;

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

  public void setIsAgeRestricted(Boolean isAgeRestricted) {
    InMobiSdk.setIsAgeRestricted(isAgeRestricted);
  }

  public void init(
      final Context context,
      final String accountId,
      final JSONObject consentObject,
      final SdkInitializationListener listener) {
    InMobiSdk.init(context, accountId, consentObject, listener);
  }
}
