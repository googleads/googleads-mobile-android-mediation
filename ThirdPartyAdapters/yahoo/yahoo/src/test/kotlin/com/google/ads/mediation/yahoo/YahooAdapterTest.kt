package com.google.ads.mediation.yahoo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.mediation.Adapter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [YahooMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class YahooAdapterTest {

  private lateinit var adapter: YahooMediationAdapter

  @Test
  fun instanceOfYahooMediationAdapter_returnsAnInstanceOfAdapter() {
    adapter = YahooMediationAdapter()

    assertThat(adapter is Adapter).isTrue()
  }
}
