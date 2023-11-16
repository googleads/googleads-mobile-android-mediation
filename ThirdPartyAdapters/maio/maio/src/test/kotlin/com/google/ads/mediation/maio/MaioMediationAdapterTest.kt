package com.google.ads.mediation.maio

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Class containing unit tests for [MaioMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class MaioMediationAdapterTest {

  private var maioMediationAdapter: MaioMediationAdapter = MaioMediationAdapter()

  @Before
  fun setUp() {
    maioMediationAdapter = MaioMediationAdapter()
  }

  @Test
  fun instanceOfMaioAdapter_returnsAnInstanceOfMaioMediationAdapter() {
    assertThat(maioMediationAdapter is MaioMediationAdapter).isTrue()
  }
}
