package com.applovin.mediation;

import static com.google.common.truth.Truth.assertThat;

import com.google.ads.mediation.applovin.AppLovinMediationAdapter;
import com.google.android.gms.ads.AdError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link AppLovinUtils}. */
@RunWith(JUnit4.class)
public final class AppLovinUtilsTest {

  @Test
  public void anyErrorSendTo_getAdError_returnsAppLovinSdkErrorDomain() {
    AdError adError = AppLovinUtils.getAdError(0);
    assertThat(adError.getDomain()).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN);
    adError = AppLovinUtils.getAdError(1);
    assertThat(adError.getDomain()).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN);
    adError = AppLovinUtils.getAdError(2);
    assertThat(adError.getDomain()).isEqualTo(AppLovinMediationAdapter.APPLOVIN_SDK_ERROR_DOMAIN);
  }
}
