package com.google.ads.mediation.imobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.imobile.IMobileMediationAdapter.IMOBILE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.FailNotificationReason
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [AdapterHelper]. */
@RunWith(AndroidJUnit4::class)
class AdapterHelperTest {
  private lateinit var adError: AdError
  private var errorMessage = "Failed to request ad from Imobile: "

  @Test
  fun getAdError_codeResponse() {
    adError = AdapterHelper.getAdError(FailNotificationReason.RESPONSE)

    assertThat(adError.code).isEqualTo(0)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.RESPONSE)
  }

  @Test
  fun getAdError_codeParam() {
    adError = AdapterHelper.getAdError(FailNotificationReason.PARAM)

    assertThat(adError.code).isEqualTo(1)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.PARAM)
  }

  @Test
  fun getAdError_codeAuthority() {
    adError = AdapterHelper.getAdError(FailNotificationReason.AUTHORITY)

    assertThat(adError.code).isEqualTo(2)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.AUTHORITY)
  }

  @Test
  fun getAdError_codePermission() {
    adError = AdapterHelper.getAdError(FailNotificationReason.PERMISSION)

    assertThat(adError.code).isEqualTo(3)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.PERMISSION)
  }

  @Test
  fun getAdError_codeNetworkNotReady() {
    adError = AdapterHelper.getAdError(FailNotificationReason.NETWORK_NOT_READY)

    assertThat(adError.code).isEqualTo(4)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.NETWORK_NOT_READY)
  }

  @Test
  fun getAdError_codeNetwork() {
    adError = AdapterHelper.getAdError(FailNotificationReason.NETWORK)

    assertThat(adError.code).isEqualTo(5)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.NETWORK)
  }

  @Test
  fun getAdError_codeAdNotReady() {
    adError = AdapterHelper.getAdError(FailNotificationReason.AD_NOT_READY)

    assertThat(adError.code).isEqualTo(6)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.AD_NOT_READY)
  }

  @Test
  fun getAdError_codeNotDeliveryAd() {
    adError = AdapterHelper.getAdError(FailNotificationReason.NOT_DELIVERY_AD)

    assertThat(adError.code).isEqualTo(7)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.NOT_DELIVERY_AD)
  }

  @Test
  fun getAdError_codeShowTimeout() {
    adError = AdapterHelper.getAdError(FailNotificationReason.SHOW_TIMEOUT)

    assertThat(adError.code).isEqualTo(8)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.SHOW_TIMEOUT)
  }

  @Test
  fun getAdError_codeUnknown() {
    adError = AdapterHelper.getAdError(FailNotificationReason.UNKNOWN)

    assertThat(adError.code).isEqualTo(9)
    assertThat(adError.domain).isEqualTo(IMOBILE_SDK_ERROR_DOMAIN)
    assertThat(adError.message).isEqualTo(errorMessage + FailNotificationReason.UNKNOWN)
  }
}
