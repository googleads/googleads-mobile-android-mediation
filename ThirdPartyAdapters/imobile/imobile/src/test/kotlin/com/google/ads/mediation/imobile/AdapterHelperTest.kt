package com.google.ads.mediation.imobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.imobile.IMobileMediationAdapter.IMOBILE_SDK_ERROR_DOMAIN
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [AdapterHelper]. */
@RunWith(AndroidJUnit4::class)
class AdapterHelperTest {
  private val errorMessage = "Failed to request ad from Imobile: "

  @Test
  fun getAdError_codeResponse() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    assertThat(adError)
      .hasCode(0)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.RESPONSE)
  }

  @Test
  fun getAdError_codeParam() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.PARAM)

    assertThat(adError)
      .hasCode(1)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.PARAM)
  }

  @Test
  fun getAdError_codeAuthority() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.AUTHORITY)

    assertThat(adError)
      .hasCode(2)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.AUTHORITY)
  }

  @Test
  fun getAdError_codePermission() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.PERMISSION)

    assertThat(adError)
      .hasCode(3)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.PERMISSION)
  }

  @Test
  fun getAdError_codeNetworkNotReady() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.NETWORK_NOT_READY)

    assertThat(adError)
      .hasCode(4)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.NETWORK_NOT_READY)
  }

  @Test
  fun getAdError_codeNetwork() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.NETWORK)

    assertThat(adError)
      .hasCode(5)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.NETWORK)
  }

  @Test
  fun getAdError_codeAdNotReady() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.AD_NOT_READY)

    assertThat(adError)
      .hasCode(6)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.AD_NOT_READY)
  }

  @Test
  fun getAdError_codeNotDeliveryAd() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.NOT_DELIVERY_AD)

    assertThat(adError)
      .hasCode(7)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.NOT_DELIVERY_AD)
  }

  @Test
  fun getAdError_codeShowTimeout() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.SHOW_TIMEOUT)

    assertThat(adError)
      .hasCode(8)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.SHOW_TIMEOUT)
  }

  @Test
  fun getAdError_codeUnknown() {
    val adError = AdapterHelper.getAdError(FailNotificationReason.UNKNOWN)

    assertThat(adError)
      .hasCode(9)
      .hasDomain(IMOBILE_SDK_ERROR_DOMAIN)
      .hasMessage(errorMessage + FailNotificationReason.UNKNOWN)
  }
}
