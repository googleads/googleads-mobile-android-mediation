package com.google.ads.mediation.chartboost

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.StartError
import com.google.ads.mediation.chartboost.ChartboostConstants.CHARTBOOST_SDK_ERROR_DOMAIN
import com.google.ads.mediation.chartboost.ChartboostConstants.ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ChartboostConstantsTest {

  private lateinit var adError: AdError
  private val cacheCode: CacheError.Code = mock()
  private val showCode: ShowError.Code = mock()
  private val clickCode: ClickError.Code = mock()
  private val startCode: StartError.Code = mock()

  private val cacheError: CacheError = mock()
  private val showError: ShowError = mock()
  private val clickError: ClickError = mock()
  private val startError: StartError = mock()

  @Test
  fun createAdapterError_returnsValidAdError() {
    adError = ChartboostConstants.createAdapterError(ERROR_CODE, ERROR_MESSAGE)

    assertThat(adError.code).isEqualTo(ERROR_CODE)
    assertThat(adError.message).isEqualTo(ERROR_MESSAGE)
    assertThat(adError.domain).isEqualTo(ERROR_DOMAIN)
  }

  @Test
  fun createSdkError_cacheError() {
    whenever(cacheError.code) doReturn cacheCode
    whenever(cacheCode.errorCode) doReturn ERROR_CODE
    whenever(cacheError.toString()) doReturn ERROR_MESSAGE

    adError = ChartboostConstants.createSDKError(cacheError)

    assertCommon(adError)
  }

  @Test
  fun createSdkError_showError() {
    whenever(showError.code) doReturn showCode
    whenever(showCode.errorCode) doReturn ERROR_CODE
    whenever(showError.toString()) doReturn ERROR_MESSAGE

    adError = ChartboostConstants.createSDKError(showError)

    assertCommon(adError)
  }

  @Test
  fun createSdkError_clickError() {
    whenever(clickError.code) doReturn clickCode
    whenever(clickCode.errorCode) doReturn ERROR_CODE
    whenever(clickError.toString()) doReturn ERROR_MESSAGE

    adError = ChartboostConstants.createSDKError(clickError)

    assertCommon(adError)
  }

  @Test
  fun createSdkError_startError() {
    whenever(startError.code) doReturn startCode
    whenever(startCode.errorCode) doReturn ERROR_CODE
    whenever(startError.toString()) doReturn ERROR_MESSAGE

    adError = ChartboostConstants.createSDKError(startError)

    assertCommon(adError)
  }

  private fun assertCommon(adError: AdError) {
    assertThat(adError.code).isEqualTo(ERROR_CODE)
    assertThat(adError.message).isEqualTo(ERROR_MESSAGE)
    assertThat(adError.domain).isEqualTo(CHARTBOOST_SDK_ERROR_DOMAIN)
  }

  private companion object {
    const val ERROR_CODE: Int = 2
    const val ERROR_MESSAGE: String = "err_message"
  }
}
