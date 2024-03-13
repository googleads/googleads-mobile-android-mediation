package com.google.ads.mediation.applovin;

import com.applovin.sdk.AppLovinSdkUtils;

/**
 * Wrapper class to enable mocking of {@link AppLovinSdkUtils#runOnUiThread} for unit testing.
 *
 * <p><b>Note:</b> It is used as a layer between the AppLovin Adapter's and the AppLovin SDK. It is
 * required to use this class instead of calling the AppLovin SDK methods directly. More background:
 * http://yaqs/6706506443522048
 */
public class AppLovinSdkUtilsWrapper {

  public void runOnUiThread (Runnable runnable) {
    AppLovinSdkUtils.runOnUiThread(runnable);
  }
}
