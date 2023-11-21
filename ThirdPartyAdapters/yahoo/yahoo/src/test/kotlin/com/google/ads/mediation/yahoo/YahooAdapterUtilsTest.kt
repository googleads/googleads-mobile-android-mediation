package com.google.ads.mediation.yahoo

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.mediation.MediationAdConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [YahooAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class YahooAdapterUtilsTest {

  private val mockMediationAdConfiguration = mock<MediationAdConfiguration>()

  @Test
  fun getSiteId_mediationExtras_returnsSiteId() {
    val mediationExtras = bundleOf(YahooAdapterUtils.SITE_KEY to "foobar")
    assertThat(YahooAdapterUtils.getSiteId(null, mediationExtras)).isEqualTo("foobar")
  }

  @Test
  fun getSiteId_serverParams_returnsSiteId() {
    val serverParams = bundleOf(YahooAdapterUtils.SITE_KEY to "barfoo")
    assertThat(YahooAdapterUtils.getSiteId(serverParams, Bundle())).isEqualTo("barfoo")
  }

  @Test
  fun getSiteId_emptyBundles_returnsNull() {
    assertThat(YahooAdapterUtils.getSiteId(Bundle(), Bundle())).isNull()
  }

  @Test
  fun getSiteId_mediationExtras_returnsDCNKey() {
    val mediationExtras = bundleOf(YahooAdapterUtils.DCN_KEY to "foobar")
    assertThat(YahooAdapterUtils.getSiteId(null, mediationExtras)).isEqualTo("foobar")
  }

  @Test
  fun getSiteId_serverParams_returnsDCNKey() {
    val serverParams = bundleOf(YahooAdapterUtils.DCN_KEY to "barfoo")
    assertThat(YahooAdapterUtils.getSiteId(serverParams, Bundle())).isEqualTo("barfoo")
  }

  @Test
  fun getSiteIdWithAdConfig_nullParams_returnsNull() {
    assertThat(YahooAdapterUtils.getSiteId(null, mockMediationAdConfiguration)).isNull()
  }

  @Test
  fun getSiteIdWithAdConfig_serverParams_returnsSiteId() {
    val serverParams = bundleOf(YahooAdapterUtils.SITE_KEY to "barfoo")
    val mediationExtras = bundleOf(YahooAdapterUtils.SITE_KEY to "foobar")
    whenever(mockMediationAdConfiguration.mediationExtras) doReturn mediationExtras

    assertThat(YahooAdapterUtils.getSiteId(serverParams, mockMediationAdConfiguration))
      .isEqualTo("barfoo")
  }

  @Test
  fun getSiteIdWithAdConfig_serverParams_returnsDCNKey() {
    val serverParams = bundleOf(YahooAdapterUtils.DCN_KEY to "barfoo")
    val mediationExtras = bundleOf(YahooAdapterUtils.DCN_KEY to "foobar")
    whenever(mockMediationAdConfiguration.mediationExtras) doReturn mediationExtras

    assertThat(YahooAdapterUtils.getSiteId(serverParams, mockMediationAdConfiguration))
      .isEqualTo("barfoo")
  }

  @Test
  fun getSiteIdWithAdConfig_mediationExtras_returnsDCNKey() {
    val mediationExtras = bundleOf(YahooAdapterUtils.DCN_KEY to "foobar")
    whenever(mockMediationAdConfiguration.mediationExtras) doReturn mediationExtras

    assertThat(YahooAdapterUtils.getSiteId(null, mockMediationAdConfiguration)).isEqualTo("foobar")
  }

  @Test
  fun getSiteIdWithAdConfig_mediationExtras_returnsSiteId() {
    val mediationExtras = bundleOf(YahooAdapterUtils.SITE_KEY to "foobar")
    whenever(mockMediationAdConfiguration.mediationExtras) doReturn mediationExtras

    assertThat(YahooAdapterUtils.getSiteId(null, mockMediationAdConfiguration)).isEqualTo("foobar")
  }
}
