package com.google.ads.mediation.inmobi

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class InMobiAdapterTest {

  private val inMobiInitializer = mock<InMobiInitializer>()
  private val inMobiAdFactory = mock<InMobiAdFactory>()
  private val inMobiSdkWrapper = mock<InMobiSdkWrapper>()

  private lateinit var adapter: InMobiAdapter

  @Test
  fun instanceOfInMobiAdapter_returnsAnInstanceOfInMobiMediationAdapter() {
    adapter = InMobiAdapter()

    assertThat(adapter is InMobiMediationAdapter)
  }
}
