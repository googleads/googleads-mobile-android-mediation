package com.google.ads.mediation.ironsource

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [IronSourceAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceAdapterTest {

  private lateinit var adapter: IronSourceAdapter

  @Test
  fun instanceOfIronSourceAdapter_returnsAnInstanceOfIronSourceMediationAdapter() {
    adapter = IronSourceAdapter()

    assertThat(adapter is IronSourceMediationAdapter).isTrue()
  }
}
