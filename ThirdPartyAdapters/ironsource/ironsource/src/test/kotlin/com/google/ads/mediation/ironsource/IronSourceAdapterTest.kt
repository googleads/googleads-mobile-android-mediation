package com.google.ads.mediation.ironsource

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertIs
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [IronSourceAdapter]. */
@RunWith(AndroidJUnit4::class)
class IronSourceAdapterTest {

  private lateinit var adapter: IronSourceAdapter

  @Test
  fun instanceOfIronSourceAdapter_returnsAnInstanceOfIronSourceMediationAdapter() {
    adapter = IronSourceAdapter()

    assertIs<IronSourceMediationAdapter>(adapter)
  }
}
