package com.google.ads.mediation.vungle

import com.google.common.truth.Truth.assertThat
import com.vungle.mediation.VungleAdapter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [VungleAdapter]. */
@RunWith(JUnit4::class)
class VungleAdapterTest {
  private lateinit var adapter: VungleMediationAdapter

  @Test
  fun instanceOfVungleAdapter_returnsAnInstanceOfVungleMediationAdapter() {
    adapter = VungleAdapter()

    assertThat(adapter is VungleMediationAdapter).isTrue()
  }
}
