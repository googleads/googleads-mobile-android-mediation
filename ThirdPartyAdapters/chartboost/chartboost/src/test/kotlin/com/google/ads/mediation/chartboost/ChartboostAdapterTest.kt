package com.google.ads.mediation.chartboost

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [ChartboostAdapter]. */
@RunWith(AndroidJUnit4::class)
class ChartboostAdapterTest {

  private lateinit var adapter: ChartboostAdapter

  @Test
  fun instanceOfChartboostMediationAdapter() {
    adapter = ChartboostAdapter()

    assertThat(adapter is ChartboostMediationAdapter).isTrue()
  }
}
