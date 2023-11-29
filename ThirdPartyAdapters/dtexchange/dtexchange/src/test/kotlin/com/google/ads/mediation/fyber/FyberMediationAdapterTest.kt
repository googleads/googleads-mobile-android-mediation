package com.google.ads.mediation.fyber

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FyberMediationAdapterTest {

  private val adapter = FyberMediationAdapter()

  @Test
  fun dummyTest() {
    assertThat(adapter is FyberMediationAdapter).isTrue()
  }
}
